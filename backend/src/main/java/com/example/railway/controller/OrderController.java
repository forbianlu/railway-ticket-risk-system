package com.example.railway.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.OrderResponse;
import com.example.railway.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PostMapping("/{id}/refund")
    public OrderResponse refund(@PathVariable("id") Long orderId) {
        return orderService.refund(orderId);
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(value = "userId", required = false) Long userId) {
        return orderService.listOrders(userId);
    }
}
