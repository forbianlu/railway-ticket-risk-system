package com.example.railway.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {

    private final OrderService orderService;

    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${railway.order.close-expired-delay-ms:60000}")
    public void closeExpiredOrders() {
        orderService.closeExpiredOrders();
    }
}
