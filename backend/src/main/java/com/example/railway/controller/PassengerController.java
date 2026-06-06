package com.example.railway.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.OrderDetailResponse;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.PassengerCreateOrderRequest;
import com.example.railway.dto.PassengerSummaryResponse;
import com.example.railway.dto.PassengerTravelerRequest;
import com.example.railway.dto.PassengerTravelerResponse;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.security.AuthContext;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.PassengerService;
import com.example.railway.service.RateLimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Passenger API", description = "Passenger order, payment, refund and traveler profile APIs")
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

    @Operation(summary = "Query current passenger summary")
    @GetMapping("/summary")
    public PassengerSummaryResponse summary() {
        return passengerService.summary();
    }

    @Operation(summary = "Query my orders")
    @GetMapping("/orders")
    public OrderPageResponse orders(@RequestParam(value = "status", required = false) String status,
                                    @RequestParam(value = "page", required = false) Integer page,
                                    @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listOrders(status, page, size);
    }

    @Operation(summary = "Query my order detail")
    @GetMapping("/orders/{id}/detail")
    public OrderDetailResponse orderDetail(@PathVariable("id") Long orderId) {
        return passengerService.orderDetail(orderId);
    }

    @Operation(summary = "Query my traveler profiles")
    @GetMapping("/travelers")
    public List<PassengerTravelerResponse> travelers() {
        return passengerService.listTravelers();
    }

    @Operation(summary = "Create my traveler profile")
    @PostMapping("/travelers")
    public PassengerTravelerResponse createTraveler(@Valid @RequestBody PassengerTravelerRequest request) {
        return passengerService.createTraveler(request);
    }

    @Operation(summary = "Update my traveler profile")
    @PutMapping("/travelers/{id}")
    public PassengerTravelerResponse updateTraveler(@PathVariable("id") Long travelerId,
                                                    @Valid @RequestBody PassengerTravelerRequest request) {
        return passengerService.updateTraveler(travelerId, request);
    }

    @Operation(summary = "Delete my traveler profile")
    @DeleteMapping("/travelers/{id}")
    public void deleteTraveler(@PathVariable("id") Long travelerId) {
        passengerService.deleteTraveler(travelerId);
    }

    @Operation(summary = "Set default traveler profile")
    @PostMapping("/travelers/{id}/default")
    public PassengerTravelerResponse setDefaultTraveler(@PathVariable("id") Long travelerId) {
        return passengerService.setDefaultTraveler(travelerId);
    }

    @Operation(summary = "Create passenger order and lock inventory")
    @PostMapping("/orders")
    public OrderResponse createOrder(@Valid @RequestBody PassengerCreateOrderRequest request,
                                     HttpServletRequest httpRequest) {
        String key = AuthContext.currentOrNull() == null
                ? "rate:passenger:order:create:ip:" + httpRequest.getRemoteAddr()
                : "rate:passenger:order:create:user:" + AuthContext.current().getUserId();
        rateLimitService.check("order-create", key);
        return passengerService.createOrder(request);
    }

    @Operation(summary = "Pay my pending order")
    @PostMapping("/orders/{id}/pay")
    public OrderResponse pay(@PathVariable("id") Long orderId) {
        return passengerService.payOrder(orderId);
    }

    @Operation(summary = "Close my pending order")
    @PostMapping("/orders/{id}/close")
    public OrderResponse close(@PathVariable("id") Long orderId) {
        return passengerService.closeOrder(orderId);
    }

    @Operation(summary = "Refund my paid order")
    @PostMapping("/orders/{id}/refund")
    public OrderResponse refund(@PathVariable("id") Long orderId) {
        return passengerService.refundOrder(orderId);
    }

    @Operation(summary = "Query my payment records")
    @GetMapping("/payments")
    public PaymentPageResponse payments(@RequestParam(value = "status", required = false) String status,
                                        @RequestParam(value = "page", required = false) Integer page,
                                        @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listPayments(status, page, size);
    }

    @Operation(summary = "Query my refund records")
    @GetMapping("/refunds")
    public RefundPageResponse refunds(@RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listRefunds(status, page, size);
    }
}
