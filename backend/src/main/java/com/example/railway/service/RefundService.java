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
import com.example.railway.config.RefundCallbackProperties;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.PaymentStatus;
import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.RefundStatus;
import com.example.railway.domain.TicketOrder;
import com.example.railway.dto.RefundCallbackRequest;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.dto.RefundResponse;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@Service
public class RefundService {

    private static final String MOCK_CHANNEL = "MOCK";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final RefundRecordRepository refundRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final OperationLogService operationLogService;
    private final CallbackSignatureService callbackSignatureService;
    private final RefundCallbackProperties refundCallbackProperties;
    private final OutboxEventPublisher outboxEventPublisher;
    private final NotificationService notificationService;

    public RefundService(RefundRecordRepository refundRecordRepository,
                         PaymentRecordRepository paymentRecordRepository,
                         OperationLogService operationLogService,
                         CallbackSignatureService callbackSignatureService,
                         RefundCallbackProperties refundCallbackProperties,
                         OutboxEventPublisher outboxEventPublisher,
                         NotificationService notificationService) {
        this.refundRecordRepository = refundRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.operationLogService = operationLogService;
        this.callbackSignatureService = callbackSignatureService;
        this.refundCallbackProperties = refundCallbackProperties;
        this.outboxEventPublisher = outboxEventPublisher;
        this.notificationService = notificationService;
    }

    @Transactional
    public RefundResponse createForRefundedOrder(TicketOrder order) {
        String requestId = "refund-order-" + order.getId();
        RefundRecord existing = refundRecordRepository.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            return RefundResponse.from(existing);
        }
        RefundRecord pending = refundRecordRepository.findByOrderIdAndStatus(order.getId(), RefundStatus.PENDING).orElse(null);
        if (pending != null) {
            return RefundResponse.from(pending);
        }
        RefundRecord success = refundRecordRepository.findByOrderIdAndStatus(order.getId(), RefundStatus.SUCCESS).orElse(null);
        if (success != null) {
            return RefundResponse.from(success);
        }

        PaymentRecord payment = paymentRecordRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.SUCCESS)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        RefundRecord refund = new RefundRecord();
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentNo(payment == null ? null : payment.getPaymentNo());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setAmount(order.getAmount());
        refund.setStatus(RefundStatus.PENDING);
        refund.setChannel(MOCK_CHANNEL);
        refund.setRequestId(requestId);
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        RefundRecord saved = refundRecordRepository.save(refund);
        operationLogService.record(
                "USER-" + order.getUserId(),
                "CREATE_REFUND",
                "REFUND",
                saved.getRefundNo(),
                "创建退款流水 " + saved.getRefundNo() + "，关联订单 " + order.getOrderNo()
        );
        publishRefundEvent(OutboxEventTypes.REFUND_CREATED, saved);
        return RefundResponse.from(saved);
    }

    @Transactional
    public RefundResponse createForTicketChange(TicketOrder order, java.math.BigDecimal amount, String requestId) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedRequestId = normalizeText(requestId);
        RefundRecord existing = normalizedRequestId == null ? null : refundRecordRepository.findByRequestId(normalizedRequestId).orElse(null);
        if (existing != null) {
            return RefundResponse.from(existing);
        }
        PaymentRecord payment = paymentRecordRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.SUCCESS)
                .orElse(null);
        LocalDateTime now = LocalDateTime.now();
        RefundRecord refund = new RefundRecord();
        refund.setRefundNo(generateRefundNo());
        refund.setPaymentNo(payment == null ? null : payment.getPaymentNo());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setAmount(amount);
        refund.setStatus(RefundStatus.PENDING);
        refund.setChannel(MOCK_CHANNEL);
        refund.setRequestId(normalizedRequestId);
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        RefundRecord saved = refundRecordRepository.save(refund);
        operationLogService.record(
                "USER-" + order.getUserId(),
                "CREATE_CHANGE_REFUND",
                "REFUND",
                saved.getRefundNo(),
                "Ticket change refund created for order " + order.getOrderNo()
        );
        publishRefundEvent(OutboxEventTypes.REFUND_CREATED, saved);
        return RefundResponse.from(saved);
    }

    @Transactional
    public RefundResponse handleCallback(RefundCallbackRequest request) {
        String callbackRequestId = normalizeText(request.getCallbackRequestId());
        RefundRecord existingCallback = refundRecordRepository.findByCallbackRequestId(callbackRequestId).orElse(null);
        if (existingCallback != null) {
            return RefundResponse.from(existingCallback);
        }
        RefundRecord record = refundRecordRepository.findByRefundNo(normalizeText(request.getRefundNo()))
                .orElseThrow(() -> new BusinessException("退款流水不存在"));
        validateCallback(request, record);
        if (RefundStatus.SUCCESS.equals(record.getStatus())) {
            return RefundResponse.from(record);
        }
        if (RefundStatus.FAILED.equals(record.getStatus()) && Boolean.TRUE.equals(request.getSuccess())) {
            throw new BusinessException("退款流水已失败，不能改为成功");
        }
        if (RefundStatus.FAILED.equals(record.getStatus())) {
            return RefundResponse.from(record);
        }
        if (Boolean.TRUE.equals(request.getSuccess())) {
            return RefundResponse.from(markRefundSuccess(record, callbackRequestId, request.getChannelRefundNo(), request.getMessage()));
        }
        return RefundResponse.from(markRefundFailed(record, callbackRequestId, request.getMessage()));
    }

    @Transactional(readOnly = true)
    public RefundPageResponse listRefunds(Long orderId, String refundNo, String status, Integer page, Integer size) {
        return listRefundsInternal(null, orderId, refundNo, status, page, size);
    }

    @Transactional(readOnly = true)
    public RefundPageResponse listRefundsForUser(Long userId, String status, Integer page, Integer size) {
        return listRefundsInternal(userId, null, null, status, page, size);
    }

    private RefundPageResponse listRefundsInternal(Long userId, Long orderId, String refundNo, String status, Integer page, Integer size) {
        final String normalizedRefundNo = normalizeText(refundNo);
        final RefundStatus refundStatus = parseRefundStatus(status);
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        Page<RefundRecord> refundPage = refundRecordRepository.findAll(
                buildRefundSpecification(userId, orderId, normalizedRefundNo, refundStatus),
                pageRequest
        );
        return RefundPageResponse.from(refundPage);
    }

    public RefundCallbackRequest buildMockCallback(String refundNo,
                                                   String callbackRequestId,
                                                   Boolean success,
                                                   String channelRefundNo,
                                                   String message) {
        RefundRecord record = refundRecordRepository.findByRefundNo(normalizeText(refundNo))
                .orElseThrow(() -> new BusinessException("退款流水不存在"));
        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setRefundNo(record.getRefundNo());
        request.setCallbackRequestId(callbackRequestId);
        request.setSuccess(success);
        request.setChannelRefundNo(channelRefundNo);
        request.setAmount(record.getAmount());
        request.setMessage(message);
        request.setTimestamp(callbackSignatureService.currentTimestamp());
        request.setSignature(callbackSignatureService.sign(
                callbackSignatureService.refundPlainText(
                        request.getRefundNo(),
                        request.getCallbackRequestId(),
                        request.getAmount(),
                        request.getSuccess(),
                        request.getTimestamp()
                ),
                refundCallbackProperties.getCallbackSecret()
        ));
        return request;
    }

    private RefundRecord markRefundSuccess(RefundRecord record, String callbackRequestId, String channelRefundNo, String message) {
        LocalDateTime now = LocalDateTime.now();
        record.setStatus(RefundStatus.SUCCESS);
        record.setCallbackRequestId(callbackRequestId);
        record.setChannelRefundNo(normalizeText(channelRefundNo));
        record.setCallbackMessage(normalizeCallbackMessage(message, "mock refund success"));
        record.setRefundedAt(now);
        record.setUpdatedAt(now);
        RefundRecord saved = refundRecordRepository.save(record);
        operationLogService.record(
                "REFUND_CALLBACK",
                "REFUND_SUCCESS",
                "REFUND",
                saved.getRefundNo(),
                "退款回调成功，订单 " + saved.getOrderNo() + " 已完成资金退款"
        );
        publishRefundEvent(OutboxEventTypes.REFUND_SUCCEEDED, saved);
        notificationService.notifyRefundSucceeded(saved);
        return saved;
    }

    private RefundRecord markRefundFailed(RefundRecord record, String callbackRequestId, String message) {
        LocalDateTime now = LocalDateTime.now();
        record.setStatus(RefundStatus.FAILED);
        record.setCallbackRequestId(callbackRequestId);
        record.setCallbackMessage(normalizeCallbackMessage(message, "mock refund failed"));
        record.setUpdatedAt(now);
        RefundRecord saved = refundRecordRepository.save(record);
        operationLogService.record(
                "REFUND_CALLBACK",
                "REFUND_FAILED",
                "REFUND",
                saved.getRefundNo(),
                "退款回调失败，订单 " + saved.getOrderNo() + " 需要后续处理"
        );
        publishRefundEvent(OutboxEventTypes.REFUND_FAILED, saved);
        notificationService.notifyRefundFailed(saved);
        return saved;
    }

    private void publishRefundEvent(String eventType, RefundRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("refundNo", record.getRefundNo());
        payload.put("paymentNo", record.getPaymentNo());
        payload.put("orderId", record.getOrderId());
        payload.put("orderNo", record.getOrderNo());
        payload.put("userId", record.getUserId());
        payload.put("amount", record.getAmount());
        payload.put("status", record.getStatus() == null ? null : record.getStatus().name());
        payload.put("channelRefundNo", record.getChannelRefundNo());
        outboxEventPublisher.publish(eventType, "REFUND", record.getRefundNo(), payload);
    }

    private void validateCallback(RefundCallbackRequest request, RefundRecord record) {
        String plainText = callbackSignatureService.refundPlainText(
                request.getRefundNo(),
                request.getCallbackRequestId(),
                request.getAmount(),
                request.getSuccess(),
                request.getTimestamp()
        );
        callbackSignatureService.verify(
                plainText,
                request.getSignature(),
                request.getTimestamp(),
                refundCallbackProperties.getCallbackSecret(),
                refundCallbackProperties.isSignatureEnabled(),
                refundCallbackProperties.getTimestampToleranceSeconds()
        );
        if (request.getAmount() == null || record.getAmount().compareTo(request.getAmount()) != 0) {
            operationLogService.record(
                    "REFUND_CALLBACK",
                    "REFUND_AMOUNT_MISMATCH",
                    "REFUND",
                    record.getRefundNo(),
                    "退款回调金额不一致，订单 " + record.getOrderNo()
            );
            throw new BusinessException("退款回调金额不一致");
        }
        if (Boolean.TRUE.equals(request.getSuccess()) && normalizeText(request.getChannelRefundNo()) == null) {
            throw new BusinessException("退款成功回调缺少渠道退款号");
        }
    }

    private Specification<RefundRecord> buildRefundSpecification(final Long userId,
                                                                 final Long orderId,
                                                                 final String refundNo,
                                                                 final RefundStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            if (orderId != null) {
                predicates.add(criteriaBuilder.equal(root.get("orderId"), orderId));
            }
            if (refundNo != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("refundNo")),
                        "%" + refundNo.toLowerCase() + "%"
                ));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private RefundStatus parseRefundStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }
        try {
            return RefundStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("退款状态不合法: " + status);
        }
    }

    private String generateRefundNo() {
        String refundNo;
        do {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            refundNo = "RF" + timestamp + random;
        } while (refundRecordRepository.existsByRefundNo(refundNo));
        return refundNo;
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
}
