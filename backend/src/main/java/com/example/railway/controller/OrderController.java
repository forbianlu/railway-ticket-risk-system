package com.example.railway.controller;

import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.service.OrderService;
import com.example.railway.service.RateLimitService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final RateLimitService rateLimitService;

    public OrderController(OrderService orderService,
                           RateLimitService rateLimitService) {
        this.orderService = orderService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request,
                                     HttpServletRequest httpRequest) {
        String requester = request.getUserId() == null ? "ip:" + httpRequest.getRemoteAddr() : "user:" + request.getUserId();
        rateLimitService.check("rate:order:create:" + requester, 10, 60);
        return orderService.createOrder(request);
    }

    @PostMapping("/{id}/refund")
    public OrderResponse refund(@PathVariable("id") Long orderId) {
        return orderService.refund(orderId);
    }

    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable("id") Long orderId) {
        return orderService.pay(orderId);
    }

    @PostMapping("/{id}/close")
    public OrderResponse close(@PathVariable("id") Long orderId) {
        return orderService.closeUnpaidOrder(orderId);
    }

    @PostMapping("/close-expired")
    public List<OrderResponse> closeExpiredOrders() {
        return orderService.closeExpiredOrders();
    }

    @GetMapping
    public OrderPageResponse listOrders(@RequestParam(value = "userId", required = false) Long userId,
                                        @RequestParam(value = "status", required = false) String status,
                                        @RequestParam(value = "fromDate", required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                        @RequestParam(value = "toDate", required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                        @RequestParam(value = "orderNo", required = false) String orderNo,
                                        @RequestParam(value = "page", required = false) Integer page,
                                        @RequestParam(value = "size", required = false) Integer size) {
        return orderService.listOrders(userId, status, fromDate, toDate, orderNo, page, size);
    }
}
