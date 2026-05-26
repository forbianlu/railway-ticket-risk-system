package com.example.railway.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.MockRefundCallbackRequest;
import com.example.railway.dto.RefundCallbackRequest;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.dto.RefundResponse;
import com.example.railway.service.RefundService;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public RefundPageResponse listRefunds(@RequestParam(value = "orderId", required = false) Long orderId,
                                          @RequestParam(value = "refundNo", required = false) String refundNo,
                                          @RequestParam(value = "status", required = false) String status,
                                          @RequestParam(value = "page", required = false) Integer page,
                                          @RequestParam(value = "size", required = false) Integer size) {
        return refundService.listRefunds(orderId, refundNo, status, page, size);
    }

    @PostMapping("/callback")
    public RefundResponse callback(@Valid @RequestBody RefundCallbackRequest request) {
        return refundService.handleCallback(request);
    }

    @PostMapping("/callback/mock")
    public RefundResponse mockCallback(@Valid @RequestBody MockRefundCallbackRequest request) {
        RefundCallbackRequest signedRequest = refundService.buildMockCallback(
                request.getRefundNo(),
                request.getCallbackRequestId(),
                request.getSuccess(),
                request.getChannelRefundNo(),
                request.getMessage()
        );
        return refundService.handleCallback(signedRequest);
    }
}
