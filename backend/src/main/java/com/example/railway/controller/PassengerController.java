package com.example.railway.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.PassengerCreateOrderRequest;
import com.example.railway.dto.PassengerSummaryResponse;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.security.AuthContext;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.PassengerService;
import com.example.railway.service.RateLimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "乘客端接口", description = "普通乘客的订单、支付、退票和个人流水查询")
@RequiredRole(UserRole.USER)
@RestController
@RequestMapping("/api/passenger")
public class PassengerController {

    private final PassengerService passengerService;
    private final RateLimitService rateLimitService;

    public PassengerController(PassengerService passengerService,
                               RateLimitService rateLimitService) {
        this.passengerService = passengerService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "查询当前乘客概览")
    @GetMapping("/summary")
    public PassengerSummaryResponse summary() {
        return passengerService.summary();
    }

    @Operation(summary = "分页查询我的订单")
    @GetMapping("/orders")
    public OrderPageResponse orders(@RequestParam(value = "status", required = false) String status,
                                    @RequestParam(value = "page", required = false) Integer page,
                                    @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listOrders(status, page, size);
    }

    @Operation(summary = "乘客下单并锁定库存")
    @PostMapping("/orders")
    public OrderResponse createOrder(@Valid @RequestBody PassengerCreateOrderRequest request,
                                     HttpServletRequest httpRequest) {
        String key = AuthContext.currentOrNull() == null
                ? "rate:passenger:order:create:ip:" + httpRequest.getRemoteAddr()
                : "rate:passenger:order:create:user:" + AuthContext.current().getUserId();
        rateLimitService.check("order-create", key);
        return passengerService.createOrder(request);
    }

    @Operation(summary = "支付我的待支付订单")
    @PostMapping("/orders/{id}/pay")
    public OrderResponse pay(@PathVariable("id") Long orderId) {
        return passengerService.payOrder(orderId);
    }

    @Operation(summary = "取消我的待支付订单")
    @PostMapping("/orders/{id}/close")
    public OrderResponse close(@PathVariable("id") Long orderId) {
        return passengerService.closeOrder(orderId);
    }

    @Operation(summary = "退票我的已支付订单")
    @PostMapping("/orders/{id}/refund")
    public OrderResponse refund(@PathVariable("id") Long orderId) {
        return passengerService.refundOrder(orderId);
    }

    @Operation(summary = "分页查询我的支付流水")
    @GetMapping("/payments")
    public PaymentPageResponse payments(@RequestParam(value = "status", required = false) String status,
                                        @RequestParam(value = "page", required = false) Integer page,
                                        @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listPayments(status, page, size);
    }

    @Operation(summary = "分页查询我的退款流水")
    @GetMapping("/refunds")
    public RefundPageResponse refunds(@RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listRefunds(status, page, size);
    }
}
