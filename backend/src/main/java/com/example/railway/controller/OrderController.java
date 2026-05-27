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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "订单管理", description = "订单创建、支付、关闭、退票和分页筛选")
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

    @Operation(summary = "创建待支付订单并锁定库存")
    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request,
                                     HttpServletRequest httpRequest) {
        String requester = request.getUserId() == null ? "ip:" + httpRequest.getRemoteAddr() : "user:" + request.getUserId();
        rateLimitService.check("order-create", "rate:order:create:" + requester);
        return orderService.createOrder(request);
    }

    @Operation(summary = "退票并释放库存")
    @PostMapping("/{id}/refund")
    public OrderResponse refund(@PathVariable("id") Long orderId) {
        return orderService.refund(orderId);
    }

    @Operation(summary = "快捷模拟支付订单")
    @PostMapping("/{id}/pay")
    public OrderResponse pay(@PathVariable("id") Long orderId) {
        return orderService.pay(orderId);
    }

    @Operation(summary = "关闭待支付订单")
    @PostMapping("/{id}/close")
    public OrderResponse close(@PathVariable("id") Long orderId) {
        return orderService.closeUnpaidOrder(orderId);
    }

    @Operation(summary = "批量关闭超时待支付订单")
    @PostMapping("/close-expired")
    public List<OrderResponse> closeExpiredOrders() {
        return orderService.closeExpiredOrders();
    }

    @Operation(summary = "分页筛选订单列表")
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
