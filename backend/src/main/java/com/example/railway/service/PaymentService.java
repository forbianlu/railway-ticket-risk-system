package com.example.railway.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.config.PaymentCallbackProperties;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.PaymentStatus;
import com.example.railway.domain.TicketOrder;
import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.PaymentCallbackRequest;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@Service
public class PaymentService {

    private static final String MOCK_CHANNEL = "MOCK";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentRecordRepository paymentRecordRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final OrderService orderService;
    private final OperationLogService operationLogService;
    private final CallbackSignatureService callbackSignatureService;
    private final PaymentCallbackProperties paymentCallbackProperties;
    private final OutboxEventPublisher outboxEventPublisher;

    public PaymentService(PaymentRecordRepository paymentRecordRepository,
                          TicketOrderRepository ticketOrderRepository,
                          OrderService orderService,
                          OperationLogService operationLogService,
                          CallbackSignatureService callbackSignatureService,
                          PaymentCallbackProperties paymentCallbackProperties,
                          OutboxEventPublisher outboxEventPublisher) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.ticketOrderRepository = ticketOrderRepository;
        this.orderService = orderService;
        this.operationLogService = operationLogService;
        this.callbackSignatureService = callbackSignatureService;
        this.paymentCallbackProperties = paymentCallbackProperties;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        String requestId = normalizeText(request.getRequestId());
        if (requestId != null) {
            PaymentRecord existingByRequest = paymentRecordRepository.findByRequestId(requestId).orElse(null);
            if (existingByRequest != null) {
                return PaymentResponse.from(existingByRequest);
            }
        }

        TicketOrder order = ticketOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许创建支付流水");
        }

        PaymentRecord existingPending = paymentRecordRepository
                .findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                .orElse(null);
        if (existingPending != null) {
            return PaymentResponse.from(existingPending);
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo(generatePaymentNo());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUserId(order.getUserId());
        record.setAmount(order.getAmount());
        record.setStatus(PaymentStatus.PENDING);
        record.setChannel(MOCK_CHANNEL);
        record.setRequestId(requestId);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        PaymentRecord saved = paymentRecordRepository.save(record);
        operationLogService.record(
                "USER-" + order.getUserId(),
                "CREATE_PAYMENT",
                "PAYMENT",
                saved.getPaymentNo(),
                "创建支付流水 " + saved.getPaymentNo() + "，关联订单 " + order.getOrderNo()
        );
        return PaymentResponse.from(saved);
    }

    @Transactional
    public PaymentResponse handleCallback(PaymentCallbackRequest request) {
        String callbackRequestId = normalizeText(request.getCallbackRequestId());
        PaymentRecord existingCallback = paymentRecordRepository.findByCallbackRequestId(callbackRequestId).orElse(null);
        if (existingCallback != null) {
            return PaymentResponse.from(existingCallback);
        }

        PaymentRecord record = paymentRecordRepository.findByPaymentNo(normalizeText(request.getPaymentNo()))
                .orElseThrow(() -> new BusinessException("支付流水不存在"));
        validateCallback(request, record);
        if (PaymentStatus.SUCCESS.equals(record.getStatus())) {
            return PaymentResponse.from(record);
        }
        if (PaymentStatus.FAILED.equals(record.getStatus())) {
            throw new BusinessException("支付流水已失败，请重新创建支付流水");
        }

        if (Boolean.TRUE.equals(request.getSuccess())) {
            return PaymentResponse.from(markPaymentSuccess(record, callbackRequestId, request.getChannelPaymentNo(), request.getMessage()));
        }
        return PaymentResponse.from(markPaymentFailed(record, callbackRequestId, request.getMessage()));
    }

    @Transactional(readOnly = true)
    public PaymentPageResponse listPayments(Long orderId,
                                            String status,
                                            String paymentNo,
                                            Integer page,
                                            Integer size) {
        final PaymentStatus paymentStatus = parsePaymentStatus(status);
        final String normalizedPaymentNo = normalizeText(paymentNo);
        int pageNumber = normalizePage(page);
        int pageSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        Page<PaymentRecord> paymentPage = paymentRecordRepository.findAll(
                buildPaymentSpecification(orderId, paymentStatus, normalizedPaymentNo),
                pageRequest
        );
        return PaymentPageResponse.from(paymentPage);
    }

    public PaymentCallbackRequest buildMockCallback(String paymentNo,
                                                    String callbackRequestId,
                                                    Boolean success,
                                                    String channelPaymentNo,
                                                    String message) {
        PaymentRecord record = paymentRecordRepository.findByPaymentNo(normalizeText(paymentNo))
                .orElseThrow(() -> new BusinessException("支付流水不存在"));
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPaymentNo(record.getPaymentNo());
        request.setCallbackRequestId(callbackRequestId);
        request.setSuccess(success);
        request.setChannelPaymentNo(channelPaymentNo);
        request.setAmount(record.getAmount());
        request.setMessage(message);
        request.setTimestamp(callbackSignatureService.currentTimestamp());
        request.setSignature(callbackSignatureService.sign(
                callbackSignatureService.paymentPlainText(
                        request.getPaymentNo(),
                        request.getCallbackRequestId(),
                        request.getAmount(),
                        request.getSuccess(),
                        request.getTimestamp()
                ),
                paymentCallbackProperties.getCallbackSecret()
        ));
        return request;
    }

    private void validateCallback(PaymentCallbackRequest request, PaymentRecord record) {
        String plainText = callbackSignatureService.paymentPlainText(
                request.getPaymentNo(),
                request.getCallbackRequestId(),
                request.getAmount(),
                request.getSuccess(),
                request.getTimestamp()
        );
        callbackSignatureService.verify(
                plainText,
                request.getSignature(),
                request.getTimestamp(),
                paymentCallbackProperties.getCallbackSecret(),
                paymentCallbackProperties.isSignatureEnabled(),
                paymentCallbackProperties.getTimestampToleranceSeconds()
        );
        if (request.getAmount() == null || record.getAmount().compareTo(request.getAmount()) != 0) {
            operationLogService.record(
                    "PAYMENT_CALLBACK",
                    "PAYMENT_AMOUNT_MISMATCH",
                    "PAYMENT",
                    record.getPaymentNo(),
                    "支付回调金额不一致，订单 " + record.getOrderNo()
            );
            throw new BusinessException("支付回调金额不一致");
        }
        if (Boolean.TRUE.equals(request.getSuccess()) && normalizeText(request.getChannelPaymentNo()) == null) {
            throw new BusinessException("支付成功回调缺少渠道流水号");
        }
    }

    private PaymentRecord markPaymentSuccess(PaymentRecord record, String callbackRequestId, String channelPaymentNo, String message) {
        LocalDateTime now = LocalDateTime.now();
        record.setStatus(PaymentStatus.SUCCESS);
        record.setCallbackRequestId(callbackRequestId);
        record.setChannelPaymentNo(normalizeText(channelPaymentNo));
        record.setCallbackMessage(normalizeCallbackMessage(message, "mock payment success"));
        record.setPaidAt(now);
        record.setUpdatedAt(now);
        PaymentRecord saved = paymentRecordRepository.save(record);
        OrderResponse order = orderService.pay(saved.getOrderId());
        if (!OrderStatus.PAID.name().equals(order.getStatus())) {
            throw new BusinessException("订单未能完成支付确认");
        }
        operationLogService.record(
                "PAYMENT_CALLBACK",
                "PAYMENT_SUCCESS",
                "PAYMENT",
                saved.getPaymentNo(),
                "支付回调成功，订单 " + saved.getOrderNo() + " 已确认支付"
        );
        publishPaymentEvent(OutboxEventTypes.PAYMENT_SUCCEEDED, saved);
        return saved;
    }

    private PaymentRecord markPaymentFailed(PaymentRecord record, String callbackRequestId, String message) {
        LocalDateTime now = LocalDateTime.now();
        record.setStatus(PaymentStatus.FAILED);
        record.setCallbackRequestId(callbackRequestId);
        record.setCallbackMessage(normalizeCallbackMessage(message, "mock payment failed"));
        record.setUpdatedAt(now);
        PaymentRecord saved = paymentRecordRepository.save(record);
        operationLogService.record(
                "PAYMENT_CALLBACK",
                "PAYMENT_FAILED",
                "PAYMENT",
                saved.getPaymentNo(),
                "支付回调失败，订单 " + saved.getOrderNo() + " 保持待支付"
        );
        publishPaymentEvent(OutboxEventTypes.PAYMENT_FAILED, saved);
        return saved;
    }

    private void publishPaymentEvent(String eventType, PaymentRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("paymentNo", record.getPaymentNo());
        payload.put("orderId", record.getOrderId());
        payload.put("orderNo", record.getOrderNo());
        payload.put("userId", record.getUserId());
        payload.put("amount", record.getAmount());
        payload.put("status", record.getStatus() == null ? null : record.getStatus().name());
        payload.put("channelPaymentNo", record.getChannelPaymentNo());
        outboxEventPublisher.publish(eventType, "PAYMENT", record.getPaymentNo(), payload);
    }

    private String generatePaymentNo() {
        String paymentNo;
        do {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            paymentNo = "PAY" + timestamp + random;
        } while (paymentRecordRepository.existsByPaymentNo(paymentNo));
        return paymentNo;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCallbackMessage(String message, String fallback) {
        String normalized = normalizeText(message);
        if (normalized == null) {
            return fallback;
        }
        return normalized.length() > 200 ? normalized.substring(0, 200) : normalized;
    }

    private PaymentStatus parsePaymentStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("支付状态不合法: " + status);
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new BusinessException("页码不能小于 0");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size <= 0) {
            throw new BusinessException("每页大小必须大于 0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Specification<PaymentRecord> buildPaymentSpecification(final Long orderId,
                                                                   final PaymentStatus status,
                                                                   final String paymentNo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (orderId != null) {
                predicates.add(criteriaBuilder.equal(root.get("orderId"), orderId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (paymentNo != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("paymentNo")),
                        "%" + paymentNo.toLowerCase() + "%"
                ));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
