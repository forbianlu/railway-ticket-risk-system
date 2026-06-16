package com.example.railway.service;

import java.math.BigDecimal;
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
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.PaymentStatus;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.TicketChangeRecord;
import com.example.railway.domain.TicketChangeStatus;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.TicketRecord;
import com.example.railway.domain.TicketStatus;
import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.PassengerChangeTicketRequest;
import com.example.railway.dto.PassengerTodoItemResponse;
import com.example.railway.dto.PassengerTransactionSummaryResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.TicketChangePageResponse;
import com.example.railway.dto.TicketChangeResponse;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.TicketChangeRecordRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.repository.TicketRecordRepository;
import com.example.railway.security.AuthorizationException;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@Service
public class TicketChangeService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 100;
    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    private final TicketChangeRecordRepository ticketChangeRecordRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final TicketRecordRepository ticketRecordRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final TicketService ticketService;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;
    private final TrainSearchCacheService trainSearchCacheService;
    private final OutboxEventPublisher outboxEventPublisher;

    public TicketChangeService(TicketChangeRecordRepository ticketChangeRecordRepository,
                               TicketOrderRepository ticketOrderRepository,
                               TicketRecordRepository ticketRecordRepository,
                               SeatInventoryRepository seatInventoryRepository,
                               RefundRecordRepository refundRecordRepository,
                               TicketService ticketService,
                               PaymentService paymentService,
                               RefundService refundService,
                               OperationLogService operationLogService,
                               NotificationService notificationService,
                               TrainSearchCacheService trainSearchCacheService,
                               OutboxEventPublisher outboxEventPublisher) {
        this.ticketChangeRecordRepository = ticketChangeRecordRepository;
        this.ticketOrderRepository = ticketOrderRepository;
        this.ticketRecordRepository = ticketRecordRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.ticketService = ticketService;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.operationLogService = operationLogService;
        this.notificationService = notificationService;
        this.trainSearchCacheService = trainSearchCacheService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public TicketChangeResponse createPassengerChange(Long originalOrderId, Long userId, PassengerChangeTicketRequest request) {
        String requestId = normalizeRequired(request.getRequestId(), "requestId is required");
        TicketChangeRecord existing = ticketChangeRecordRepository.findByUserIdAndRequestId(userId, requestId).orElse(null);
        if (existing != null) {
            return response(existing);
        }

        TicketOrder original = ticketOrderRepository.findByIdAndUserId(originalOrderId, userId)
                .orElseThrow(() -> new AuthorizationException("Original order is not owned by current passenger"));
        if (!OrderStatus.PAID.equals(original.getStatus())) {
            throw new BusinessException("Only paid orders can be changed");
        }
        TicketRecord originalTicket = ticketRecordRepository.findByOrderId(original.getId()).orElse(null);
        if (originalTicket != null && !TicketStatus.ISSUED.equals(originalTicket.getStatus())) {
            throw new BusinessException("Only valid issued tickets can be changed");
        }

        SeatInventory targetInventory = seatInventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new BusinessException("Target inventory does not exist"));
        if (!targetInventory.getTrain().getId().equals(request.getTrainId())) {
            throw new BusinessException("Target train and inventory do not match");
        }
        if (original.getInventory().getId().equals(targetInventory.getId())) {
            throw new BusinessException("Please choose a different train, travel date or seat type");
        }

        targetInventory.deductOne();
        seatInventoryRepository.save(targetInventory);

        LocalDateTime now = LocalDateTime.now();
        BigDecimal priceDifference = targetInventory.getPrice().subtract(original.getAmount());
        TicketOrder newOrder = buildNewOrder(original, targetInventory, requestId, now);
        if (priceDifference.compareTo(BigDecimal.ZERO) <= 0) {
            newOrder.setStatus(OrderStatus.PAID);
            newOrder.setPaidAt(now);
        }
        TicketOrder savedNewOrder = ticketOrderRepository.save(newOrder);

        TicketChangeRecord change = new TicketChangeRecord();
        change.setChangeNo(generateChangeNo());
        change.setUserId(userId);
        change.setOriginalOrderId(original.getId());
        change.setOriginalOrderNo(original.getOrderNo());
        change.setNewOrderId(savedNewOrder.getId());
        change.setNewOrderNo(savedNewOrder.getOrderNo());
        change.setOriginalTicketNo(originalTicket == null ? null : originalTicket.getTicketNo());
        change.setOriginalTrainNo(original.getTrain().getTrainNo());
        change.setNewTrainNo(savedNewOrder.getTrain().getTrainNo());
        change.setOldAmount(original.getAmount());
        change.setNewAmount(savedNewOrder.getAmount());
        change.setPriceDifference(priceDifference);
        change.setStatus(priceDifference.compareTo(BigDecimal.ZERO) > 0 ? TicketChangeStatus.PENDING_PAYMENT : TicketChangeStatus.SUCCESS);
        change.setRequestId(requestId);
        change.setReason(normalizeText(request.getReason()));
        change.setCreatedAt(now);
        change.setUpdatedAt(now);
        TicketChangeRecord saved = ticketChangeRecordRepository.save(change);

        operationLogService.record("USER-" + userId, "CREATE_TICKET_CHANGE", "TICKET_CHANGE", saved.getChangeNo(),
                "Ticket change created from order " + original.getOrderNo() + " to order " + savedNewOrder.getOrderNo());
        publishChangeEvent(OutboxEventTypes.TICKET_CHANGE_CREATED, saved);
        notificationService.notifyTicketChangeCreated(saved);

        if (TicketChangeStatus.SUCCESS.equals(saved.getStatus())) {
            completeSuccessfulChange(saved, original, savedNewOrder, now);
        } else {
            notificationService.notifyTicketChangePendingPayment(saved);
        }
        return response(ticketChangeRecordRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public TicketChangeResponse payPassengerChange(Long changeId, Long userId) {
        TicketChangeRecord change = ticketChangeRecordRepository.findByIdAndUserId(changeId, userId)
                .orElseThrow(() -> new AuthorizationException("Ticket change is not owned by current passenger"));
        if (TicketChangeStatus.SUCCESS.equals(change.getStatus())) {
            return response(change);
        }
        if (!TicketChangeStatus.PENDING_PAYMENT.equals(change.getStatus())) {
            throw new BusinessException("Current ticket change status does not allow payment");
        }
        TicketOrder newOrder = ticketOrderRepository.findById(change.getNewOrderId())
                .orElseThrow(() -> new BusinessException("New order does not exist"));
        if (!OrderStatus.PENDING_PAYMENT.equals(newOrder.getStatus())) {
            return completeChangeIfNewOrderPaid(newOrder.getId());
        }
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(newOrder.getId());
        paymentRequest.setRequestId("change-payment-" + change.getChangeNo());
        PaymentResponse payment = paymentService.createPayment(paymentRequest);
        paymentService.handleCallback(paymentService.buildMockCallback(
                payment.getPaymentNo(),
                "change-payment-callback-" + payment.getPaymentNo(),
                true,
                "CH_CHANGE_" + payment.getPaymentNo(),
                "ticket change payment success"
        ));
        return completeChangeIfNewOrderPaid(newOrder.getId());
    }

    @Transactional
    public TicketChangeResponse completeChangeIfNewOrderPaid(Long newOrderId) {
        TicketChangeRecord change = ticketChangeRecordRepository.findByNewOrderId(newOrderId).orElse(null);
        if (change == null) {
            return null;
        }
        if (TicketChangeStatus.SUCCESS.equals(change.getStatus())) {
            return response(change);
        }
        TicketOrder original = ticketOrderRepository.findById(change.getOriginalOrderId())
                .orElseThrow(() -> new BusinessException("Original order does not exist"));
        TicketOrder newOrder = ticketOrderRepository.findById(change.getNewOrderId())
                .orElseThrow(() -> new BusinessException("New order does not exist"));
        if (!OrderStatus.PAID.equals(newOrder.getStatus())) {
            return response(change);
        }
        completeSuccessfulChange(change, original, newOrder, LocalDateTime.now());
        return response(ticketChangeRecordRepository.findById(change.getId()).orElse(change));
    }

    @Transactional(readOnly = true)
    public TicketChangePageResponse listPassengerChanges(Long userId, String status, Integer page, Integer size) {
        TicketChangeStatus parsedStatus = parseStatus(status);
        PageRequest pageRequest = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<TicketChangeRecord> changePage = parsedStatus == null
                ? ticketChangeRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
                : ticketChangeRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, parsedStatus, pageRequest);
        return TicketChangePageResponse.from(changePage);
    }

    @Transactional(readOnly = true)
    public TicketChangeResponse passengerChangeDetail(Long changeId, Long userId) {
        return response(ticketChangeRecordRepository.findByIdAndUserId(changeId, userId)
                .orElseThrow(() -> new AuthorizationException("Ticket change is not owned by current passenger")));
    }

    @Transactional(readOnly = true)
    public TicketChangePageResponse adminListChanges(String status, String changeNo, Long userId, Integer page, Integer size) {
        TicketChangeStatus parsedStatus = parseStatus(status);
        String normalizedChangeNo = normalizeText(changeNo);
        PageRequest pageRequest = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<TicketChangeRecord> changePage = ticketChangeRecordRepository.findAll(
                buildChangeSpecification(parsedStatus, normalizedChangeNo, userId),
                pageRequest
        );
        return TicketChangePageResponse.from(changePage);
    }

    @Transactional(readOnly = true)
    public TicketChangeResponse adminChangeDetail(Long changeId) {
        return response(ticketChangeRecordRepository.findById(changeId)
                .orElseThrow(() -> new BusinessException("Ticket change does not exist")));
    }

    @Transactional(readOnly = true)
    public List<TicketChangeResponse> findOrderChanges(Long orderId) {
        List<TicketChangeResponse> responses = new ArrayList<TicketChangeResponse>();
        for (TicketChangeRecord record : ticketChangeRecordRepository.findByOriginalOrderIdOrNewOrderIdOrderByCreatedAtDesc(orderId, orderId)) {
            responses.add(response(record));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public PassengerTransactionSummaryResponse passengerTransactionSummary(Long userId) {
        PassengerTransactionSummaryResponse response = new PassengerTransactionSummaryResponse();
        response.setPendingPaymentOrderCount(ticketOrderRepository.countByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT));
        response.setActiveTicketCount(ticketRecordRepository.countByUserIdAndStatus(userId, TicketStatus.ISSUED));
        response.setRefundedTicketCount(ticketRecordRepository.countByUserIdAndStatus(userId, TicketStatus.REFUNDED));
        response.setPendingChangeCount(ticketChangeRecordRepository.countByUserIdAndStatus(userId, TicketChangeStatus.PENDING_PAYMENT));
        response.setPendingRefundCount(refundRecordRepository.countByUserIdAndStatus(userId, com.example.railway.domain.RefundStatus.PENDING));
        response.setUnreadNotificationCount(notificationService.getUnreadCount(userId));
        List<OrderResponse> latestOrders = new ArrayList<OrderResponse>();
        for (TicketOrder order : ticketOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))) {
            latestOrders.add(OrderResponse.from(order));
        }
        response.setLatestOrders(latestOrders);
        List<TicketChangeResponse> latestChanges = new ArrayList<TicketChangeResponse>();
        for (TicketChangeRecord change : ticketChangeRecordRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5)).getContent()) {
            latestChanges.add(response(change));
        }
        response.setLatestChanges(latestChanges);
        response.setTodoItems(buildPassengerTodos(userId, response));
        return response;
    }

    private List<PassengerTodoItemResponse> buildPassengerTodos(Long userId, PassengerTransactionSummaryResponse summary) {
        List<PassengerTodoItemResponse> todos = new ArrayList<PassengerTodoItemResponse>();
        for (TicketOrder order : ticketOrderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, OrderStatus.PENDING_PAYMENT, PageRequest.of(0, 3))) {
            todos.add(PassengerTodoItemResponse.of(
                    "ORDER_PAYMENT",
                    "待支付订单",
                    order.getTrain().getTrainNo() + " / " + order.getOrderNo(),
                    order.getStatus().name(),
                    "HIGH",
                    "ORDER_DETAIL",
                    order.getId(),
                    null,
                    order.getCreatedAt()));
        }
        for (TicketChangeRecord change : ticketChangeRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, TicketChangeStatus.PENDING_PAYMENT, PageRequest.of(0, 3)).getContent()) {
            todos.add(PassengerTodoItemResponse.of(
                    "CHANGE_PAYMENT",
                    "待支付改签",
                    change.getOriginalTrainNo() + " -> " + change.getNewTrainNo() + " / " + change.getChangeNo(),
                    change.getStatus().name(),
                    "HIGH",
                    "CHANGE_PAY",
                    change.getNewOrderId(),
                    change.getId(),
                    change.getCreatedAt()));
        }
        if (summary.getPendingRefundCount() > 0) {
            todos.add(PassengerTodoItemResponse.of(
                    "REFUND_PENDING",
                    "退款处理中",
                    "有 " + summary.getPendingRefundCount() + " 笔退款正在处理",
                    "PENDING",
                    "MEDIUM",
                    "REFUNDS",
                    null,
                    null,
                    null));
        }
        if (summary.getUnreadNotificationCount() > 0) {
            todos.add(PassengerTodoItemResponse.of(
                    "UNREAD_NOTIFICATION",
                    "未读消息",
                    "有 " + summary.getUnreadNotificationCount() + " 条消息待查看",
                    "UNREAD",
                    "MEDIUM",
                    "NOTIFICATIONS",
                    null,
                    null,
                    null));
        }
        return todos;
    }

    private void completeSuccessfulChange(TicketChangeRecord change, TicketOrder original, TicketOrder newOrder, LocalDateTime now) {
        if (!OrderStatus.CANCELLED.equals(original.getStatus())) {
            original.setStatus(OrderStatus.CANCELLED);
            original.setClosedAt(now);
            original.getInventory().releaseOne();
            ticketOrderRepository.save(original);
            seatInventoryRepository.save(original.getInventory());
            ticketService.cancelTicketForOrder(original);
            trainSearchCacheService.evictRoute(
                    original.getTrain().getDepartureStation().getCode(),
                    original.getTrain().getArrivalStation().getCode(),
                    original.getTravelDate()
            );
        }
        TicketRecord newTicket = ticketService.issueTicketForPaidOrder(newOrder);
        change.setNewTicketNo(newTicket == null ? null : newTicket.getTicketNo());
        change.setStatus(TicketChangeStatus.SUCCESS);
        change.setCompletedAt(now);
        change.setUpdatedAt(now);
        TicketChangeRecord saved = ticketChangeRecordRepository.save(change);
        BigDecimal refundAmount = refundAmountForChange(saved);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            refundService.createForTicketChange(original, refundAmount, "change-refund-" + saved.getChangeNo());
        }
        operationLogService.record("SYSTEM", "COMPLETE_TICKET_CHANGE", "TICKET_CHANGE", saved.getChangeNo(),
                "Ticket change completed for original order " + original.getOrderNo() + " and new order " + newOrder.getOrderNo());
        publishChangeEvent(OutboxEventTypes.TICKET_CHANGE_SUCCEEDED, saved);
        notificationService.notifyTicketChangeSucceeded(saved);
    }

    private BigDecimal refundAmountForChange(TicketChangeRecord change) {
        if (change.getPriceDifference().compareTo(BigDecimal.ZERO) > 0) {
            return change.getOldAmount();
        }
        BigDecimal refund = change.getOldAmount().subtract(change.getNewAmount());
        return refund.compareTo(BigDecimal.ZERO) > 0 ? refund : BigDecimal.ZERO;
    }

    private TicketOrder buildNewOrder(TicketOrder original, SeatInventory inventory, String requestId, LocalDateTime now) {
        TicketOrder order = new TicketOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(original.getUserId());
        order.setRequestId("change-" + requestId);
        order.setPassengerName(original.getPassengerName());
        order.setPassengerIdCard(original.getPassengerIdCard());
        order.setPassengerIdType(original.getPassengerIdType());
        order.setPassengerIdNoMasked(original.getPassengerIdNoMasked());
        order.setPassengerPhoneMasked(original.getPassengerPhoneMasked());
        order.setTrain(inventory.getTrain());
        order.setInventory(inventory);
        order.setTravelDate(inventory.getTravelDate());
        order.setSeatType(inventory.getSeatType());
        order.setAmount(inventory.getPrice());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(now);
        order.setPaymentDeadlineAt(now.plusMinutes(PAYMENT_TIMEOUT_MINUTES));
        return order;
    }

    private TicketChangeResponse response(TicketChangeRecord record) {
        TicketOrder original = ticketOrderRepository.findById(record.getOriginalOrderId()).orElse(null);
        TicketOrder newOrder = ticketOrderRepository.findById(record.getNewOrderId()).orElse(null);
        return TicketChangeResponse.from(record, original, newOrder);
    }

    private void publishChangeEvent(String eventType, TicketChangeRecord change) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("changeNo", change.getChangeNo());
        payload.put("userId", change.getUserId());
        payload.put("originalOrderId", change.getOriginalOrderId());
        payload.put("newOrderId", change.getNewOrderId());
        payload.put("status", change.getStatus() == null ? null : change.getStatus().name());
        payload.put("priceDifference", change.getPriceDifference());
        outboxEventPublisher.publish(eventType, "TICKET_CHANGE", change.getChangeNo(), payload);
    }

    private Specification<TicketChangeRecord> buildChangeSpecification(final TicketChangeStatus status,
                                                                      final String changeNo,
                                                                      final Long userId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (changeNo != null) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("changeNo")), "%" + changeNo.toLowerCase() + "%"));
            }
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TicketChangeStatus parseStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }
        try {
            return TicketChangeStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid ticket change status: " + status);
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new BusinessException("Page cannot be less than 0");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size <= 0) {
            throw new BusinessException("Page size must be positive");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateChangeNo() {
        String changeNo;
        do {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            changeNo = "CG" + timestamp + random;
        } while (ticketChangeRecordRepository.existsByChangeNo(changeNo));
        return changeNo;
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "RT" + timestamp + random;
    }
}
