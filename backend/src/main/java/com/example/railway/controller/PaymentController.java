package com.example.railway.controller;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.MockPaymentCallbackRequest;
import com.example.railway.dto.PaymentCallbackRequest;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.service.PaymentService;
import com.example.railway.service.RateLimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "支付流水", description = "支付流水创建、支付回调和支付流水查询")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final RateLimitService rateLimitService;

    public PaymentController(PaymentService paymentService,
                             RateLimitService rateLimitService) {
        this.paymentService = paymentService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "创建支付流水")
    @PostMapping
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @Operation(summary = "处理支付渠道回调")
    @PostMapping("/callback")
    public PaymentResponse callback(@Valid @RequestBody PaymentCallbackRequest request,
                                    HttpServletRequest httpRequest) {
        rateLimitService.check("payment-callback", "rate:payment:callback:" + request.getPaymentNo() + ":ip:" + httpRequest.getRemoteAddr());
        return paymentService.handleCallback(request);
    }

    @Operation(summary = "构造并处理模拟支付回调")
    @PostMapping("/callback/mock")
    public PaymentResponse mockCallback(@Valid @RequestBody MockPaymentCallbackRequest request,
                                        HttpServletRequest httpRequest) {
        rateLimitService.check("payment-callback", "rate:payment:callback:" + request.getPaymentNo() + ":ip:" + httpRequest.getRemoteAddr());
        PaymentCallbackRequest signedRequest = paymentService.buildMockCallback(
                request.getPaymentNo(),
                request.getCallbackRequestId(),
                request.getSuccess(),
                request.getChannelPaymentNo(),
                request.getMessage()
        );
        return paymentService.handleCallback(signedRequest);
    }

    @Operation(summary = "分页筛选支付流水")
    @GetMapping
    public PaymentPageResponse listPayments(@RequestParam(value = "orderId", required = false) Long orderId,
                                            @RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "paymentNo", required = false) String paymentNo,
                                            @RequestParam(value = "page", required = false) Integer page,
                                            @RequestParam(value = "size", required = false) Integer size) {
        return paymentService.listPayments(orderId, status, paymentNo, page, size);
    }
}
