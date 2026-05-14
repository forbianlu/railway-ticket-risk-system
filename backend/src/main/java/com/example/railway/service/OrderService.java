package com.example.railway.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.TicketOrder;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.OrderResponse;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.TicketOrderRepository;

@Service
public class OrderService {

    private static final long PAYMENT_TIMEOUT_MINUTES = 15;

    private final TicketOrderRepository ticketOrderRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final RiskService riskService;
    private final OperationLogService operationLogService;
    private final TrainSearchCacheService trainSearchCacheService;

    public OrderService(TicketOrderRepository ticketOrderRepository,
                        SeatInventoryRepository seatInventoryRepository,
                        RiskService riskService,
                        OperationLogService operationLogService,
                        TrainSearchCacheService trainSearchCacheService) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.riskService = riskService;
        this.operationLogService = operationLogService;
        this.trainSearchCacheService = trainSearchCacheService;
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
        evictTrainSearchCacheAfterCommit(order.getInventory());
        operationLogService.record(
                "USER-" + order.getUserId(),
                "PAY_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                "订单 " + saved.getOrderNo() + " 已支付"
        );
        riskService.evaluateAfterOrderCreated(saved);
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
        evictTrainSearchCacheAfterCommit(order.getInventory());

        operationLogService.record(
                "USER-" + order.getUserId(),
                "REFUND_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                "订单 " + saved.getOrderNo() + " 已退票"
        );
        riskService.evaluateAfterRefund(saved);
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
    public List<OrderResponse> listOrders(Long userId) {
        List<TicketOrder> orders;
        if (userId == null) {
            orders = ticketOrderRepository.findTop20ByOrderByCreatedAtDesc();
        } else {
            orders = ticketOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        List<OrderResponse> responses = new ArrayList<OrderResponse>();
        for (TicketOrder order : orders) {
            responses.add(OrderResponse.from(order));
        }
        return responses;
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

    private TicketOrder closePendingOrder(TicketOrder order, LocalDateTime now, String reason) {
        order.setStatus(OrderStatus.CLOSED);
        order.setClosedAt(now);
        order.getInventory().releaseOne();
        TicketOrder saved = ticketOrderRepository.save(order);
        seatInventoryRepository.save(order.getInventory());
        evictTrainSearchCacheAfterCommit(order.getInventory());
        operationLogService.record(
                "SYSTEM",
                "CLOSE_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                reason + " " + saved.getOrderNo()
        );
        return saved;
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
