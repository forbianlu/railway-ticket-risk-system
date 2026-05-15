package com.example.railway.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.PaymentCallbackRequest;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    @PostMapping("/callback")
    public PaymentResponse callback(@Valid @RequestBody PaymentCallbackRequest request) {
        return paymentService.handleCallback(request);
    }

    @GetMapping
    public PaymentPageResponse listPayments(@RequestParam(value = "orderId", required = false) Long orderId,
                                            @RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "paymentNo", required = false) String paymentNo,
                                            @RequestParam(value = "page", required = false) Integer page,
                                            @RequestParam(value = "size", required = false) Integer size) {
        return paymentService.listPayments(orderId, status, paymentNo, page, size);
    }
}
