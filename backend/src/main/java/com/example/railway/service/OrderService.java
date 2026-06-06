package com.example.railway.service;

import java.time.LocalDate;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.railway.common.BusinessException;
import com.example.railway.common.MaskingUtils;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.PassengerIdType;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.TicketOrder;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@Service
public class OrderService {

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final TicketOrderRepository ticketOrderRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final RiskService riskService;
    private final OperationLogService operationLogService;
    private final TrainSearchCacheService trainSearchCacheService;
    private final RefundService refundService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final TicketService ticketService;

    public OrderService(TicketOrderRepository ticketOrderRepository,
                        SeatInventoryRepository seatInventoryRepository,
                        RiskService riskService,
                        OperationLogService operationLogService,
                        TrainSearchCacheService trainSearchCacheService,
                        RefundService refundService,
                        OutboxEventPublisher outboxEventPublisher,
                        TicketService ticketService) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.riskService = riskService;
        this.operationLogService = operationLogService;
        this.trainSearchCacheService = trainSearchCacheService;
        this.refundService = refundService;
        this.outboxEventPublisher = outboxEventPublisher;
        this.ticketService = ticketService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        String requestId = normalizeRequestId(request.getRequestId());
        if (requestId != null) {
            TicketOrder existingOrder = ticketOrderRepository.findByUserIdAndRequestId(request.getUserId(), requestId)
                    .orElse(null);
            if (existingOrder != null) {
                return OrderResponse.from(existingOrder);
            }
        }

        SeatInventory inventory = seatInventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new BusinessException("座位库存不存在"));

        if (!inventory.getTrain().getId().equals(request.getTrainId())) {
            throw new BusinessException("车次与库存不匹配");
        }

        inventory.deductOne();
        seatInventoryRepository.save(inventory);

        TicketOrder order = new TicketOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setRequestId(requestId);
        order.setPassengerName(request.getPassengerName());
        order.setPassengerIdCard(request.getPassengerIdCard());
        order.setPassengerIdType(parsePassengerIdType(request.getPassengerIdType()));
        order.setPassengerIdNoMasked(MaskingUtils.maskIdNo(request.getPassengerIdCard()));
        order.setPassengerPhoneMasked(MaskingUtils.maskPhone(request.getPassengerPhone()));
        order.setTrain(inventory.getTrain());
        order.setInventory(inventory);
        order.setTravelDate(inventory.getTravelDate());
        order.setSeatType(inventory.getSeatType());
        order.setAmount(inventory.getPrice());
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(now);
        order.setPaymentDeadlineAt(now.plusMinutes(PAYMENT_TIMEOUT_MINUTES));

        TicketOrder saved = ticketOrderRepository.save(order);
        evictTrainSearchCacheAfterCommit(inventory);
        operationLogService.record(
                "USER-" + request.getUserId(),
                "CREATE_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                "创建待支付订单 " + saved.getOrderNo() + "，库存已锁定"
        );
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse pay(Long orderId) {
        TicketOrder order = ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (OrderStatus.PAID.equals(order.getStatus())) {
            return OrderResponse.from(order);
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许支付");
        }

        LocalDateTime now = LocalDateTime.now();
        if (order.getPaymentDeadlineAt() != null && order.getPaymentDeadlineAt().isBefore(now)) {
            return OrderResponse.from(closePendingOrder(order, now, "订单支付超时关闭"));
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        TicketOrder saved = ticketOrderRepository.save(order);
        ticketService.issueTicketForPaidOrder(saved);
        evictTrainSearchCacheAfterCommit(order.getInventory());
        operationLogService.record(
                "USER-" + order.getUserId(),
                "PAY_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                "订单 " + saved.getOrderNo() + " 已支付"
        );
        riskService.evaluateAfterOrderCreated(saved);
        publishOrderEvent(OutboxEventTypes.ORDER_PAID, saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse refund(Long orderId) {
        TicketOrder order = ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (OrderStatus.REFUNDED.equals(order.getStatus())) {
            throw new BusinessException("订单已退票，不能重复退票");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许退票");
        }

        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        order.getInventory().releaseOne();
        TicketOrder saved = ticketOrderRepository.save(order);
        seatInventoryRepository.save(order.getInventory());
        ticketService.refundTicketForOrder(saved);
        evictTrainSearchCacheAfterCommit(order.getInventory());

        operationLogService.record(
                "USER-" + order.getUserId(),
                "REFUND_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                "订单 " + saved.getOrderNo() + " 已退票"
        );
        riskService.evaluateAfterRefund(saved);
        refundService.createForRefundedOrder(saved);
        publishOrderEvent(OutboxEventTypes.ORDER_REFUNDED, saved);
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse closeUnpaidOrder(Long orderId) {
        TicketOrder order = ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
        if (OrderStatus.CLOSED.equals(order.getStatus())) {
            throw new BusinessException("订单已关闭，不能重复关闭");
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException("当前订单状态不允许关闭");
        }
        return OrderResponse.from(closePendingOrder(order, LocalDateTime.now(), "关闭待支付订单并释放库存"));
    }

    @Transactional
    public List<OrderResponse> closeExpiredOrders() {
        List<TicketOrder> orders = ticketOrderRepository.findByStatusAndPaymentDeadlineAtBefore(
                OrderStatus.PENDING_PAYMENT,
                LocalDateTime.now()
        );
        List<OrderResponse> responses = new ArrayList<OrderResponse>();
        LocalDateTime now = LocalDateTime.now();
        for (TicketOrder order : orders) {
            responses.add(OrderResponse.from(closePendingOrder(order, now, "订单支付超时关闭")));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listOrders(Long userId,
                                        String status,
                                        LocalDate fromDate,
                                        LocalDate toDate,
                                        String orderNo,
                                        Integer page,
                                        Integer size) {
        final OrderStatus orderStatus = parseOrderStatus(status);
        final LocalDateTime startAt = fromDate == null ? null : fromDate.atStartOfDay();
        final LocalDateTime endAt = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        final String normalizedOrderNo = normalizeText(orderNo);

        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new BusinessException("创建时间范围不合法");
        }

        int pageNumber = normalizePage(page);
        int pageSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        Page<TicketOrder> orderPage = ticketOrderRepository.findAll(
                buildOrderSpecification(userId, orderStatus, startAt, endAt, normalizedOrderNo),
                pageRequest
        );
        return OrderPageResponse.from(orderPage);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "RT" + timestamp + random;
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null) {
            return null;
        }
        String normalized = requestId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private OrderStatus parseOrderStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("订单状态不合法: " + status);
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

    private Specification<TicketOrder> buildOrderSpecification(final Long userId,
                                                              final OrderStatus status,
                                                              final LocalDateTime startAt,
                                                              final LocalDateTime endAt,
                                                              final String orderNo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (startAt != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), startAt));
            }
            if (endAt != null) {
                predicates.add(criteriaBuilder.lessThan(root.<LocalDateTime>get("createdAt"), endAt));
            }
            if (orderNo != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.<String>get("orderNo")),
                        "%" + orderNo.toLowerCase() + "%"
                ));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private TicketOrder closePendingOrder(TicketOrder order, LocalDateTime now, String reason) {
        order.setStatus(OrderStatus.CLOSED);
        order.setClosedAt(now);
        order.getInventory().releaseOne();
        TicketOrder saved = ticketOrderRepository.save(order);
        seatInventoryRepository.save(order.getInventory());
        ticketService.cancelTicketForOrder(saved);
        evictTrainSearchCacheAfterCommit(order.getInventory());
        operationLogService.record(
                "SYSTEM",
                "CLOSE_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                reason + " " + saved.getOrderNo()
        );
        publishOrderEvent(OutboxEventTypes.ORDER_CLOSED, saved);
        return saved;
    }

    private void publishOrderEvent(String eventType, TicketOrder order) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNo", order.getOrderNo());
        payload.put("userId", order.getUserId());
        payload.put("status", order.getStatus() == null ? null : order.getStatus().name());
        payload.put("amount", order.getAmount());
        outboxEventPublisher.publish(eventType, "ORDER", String.valueOf(order.getId()), payload);
    }

    private PassengerIdType parsePassengerIdType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PassengerIdType.ID_CARD;
        }
        try {
            return PassengerIdType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unsupported passenger id type: " + value);
        }
    }

    private void evictTrainSearchCacheAfterCommit(SeatInventory inventory) {
        final String departureCode = inventory.getTrain().getDepartureStation().getCode();
        final String arrivalCode = inventory.getTrain().getArrivalStation().getCode();
        final LocalDate travelDate = inventory.getTravelDate();

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            trainSearchCacheService.evictRoute(departureCode, arrivalCode, travelDate);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                trainSearchCacheService.evictRoute(departureCode, arrivalCode, travelDate);
            }
        });
    }
}
