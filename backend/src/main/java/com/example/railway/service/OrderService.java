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
        order.setPassengerName(request.getPassengerName());
        order.setPassengerIdCard(request.getPassengerIdCard());
        order.setTrain(inventory.getTrain());
        order.setInventory(inventory);
        order.setTravelDate(inventory.getTravelDate());
        order.setSeatType(inventory.getSeatType());
        order.setAmount(inventory.getPrice());
        order.setStatus(OrderStatus.PAID);
        order.setCreatedAt(LocalDateTime.now());

        TicketOrder saved = ticketOrderRepository.save(order);
        evictTrainSearchCacheAfterCommit(inventory);
        operationLogService.record(
                "USER-" + request.getUserId(),
                "CREATE_ORDER",
                "ORDER",
                String.valueOf(saved.getId()),
                "创建购票订单 " + saved.getOrderNo()
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
