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
import com.example.railway.dto.NotificationPageResponse;
import com.example.railway.dto.NotificationResponse;
import com.example.railway.dto.NotificationSummaryResponse;
import com.example.railway.dto.OrderDetailResponse;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.AuthResponse;
import com.example.railway.dto.ChangePasswordRequest;
import com.example.railway.dto.PassengerChangeTicketRequest;
import com.example.railway.dto.PassengerCreateOrderRequest;
import com.example.railway.dto.PassengerProfileResponse;
import com.example.railway.dto.PassengerSummaryResponse;
import com.example.railway.dto.PassengerTransactionSummaryResponse;
import com.example.railway.dto.PassengerTravelerRequest;
import com.example.railway.dto.PassengerTravelerResponse;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.dto.TicketChangePageResponse;
import com.example.railway.dto.TicketChangeResponse;
import com.example.railway.dto.TicketPageResponse;
import com.example.railway.dto.UpdatePassengerProfileRequest;
import com.example.railway.security.AuthContext;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.NotificationService;
import com.example.railway.service.PassengerService;
import com.example.railway.service.RateLimitService;
import com.example.railway.service.TicketChangeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Passenger API", description = "Passenger order, payment, refund and traveler profile APIs")
@RequiredRole(UserRole.USER)
@RestController
@RequestMapping("/api/passenger")
public class PassengerController {

    private final PassengerService passengerService;
    private final RateLimitService rateLimitService;
    private final NotificationService notificationService;
    private final TicketChangeService ticketChangeService;

    public PassengerController(PassengerService passengerService,
                               RateLimitService rateLimitService,
                               NotificationService notificationService,
                               TicketChangeService ticketChangeService) {
        this.passengerService = passengerService;
        this.rateLimitService = rateLimitService;
        this.notificationService = notificationService;
        this.ticketChangeService = ticketChangeService;
    }

    @Operation(summary = "Query current passenger summary")
    @GetMapping("/summary")
    public PassengerSummaryResponse summary() {
        return passengerService.summary();
    }

    @Operation(summary = "Query my passenger profile")
    @GetMapping("/profile")
    public PassengerProfileResponse profile() {
        return passengerService.profile();
    }

    @Operation(summary = "Update my display name")
    @PutMapping("/profile")
    public AuthResponse updateProfile(@Valid @RequestBody UpdatePassengerProfileRequest request) {
        return passengerService.updateProfile(request.getDisplayName());
    }

    @Operation(summary = "Change my password")
    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        passengerService.changePassword(request);
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

    @Operation(summary = "Query my electronic tickets")
    @GetMapping("/tickets")
    public TicketPageResponse tickets(@RequestParam(value = "status", required = false) String status,
                                      @RequestParam(value = "page", required = false) Integer page,
                                      @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listTickets(status, page, size);
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

    @Operation(summary = "Create ticket change request for my paid order")
    @PostMapping("/orders/{id}/change")
    public TicketChangeResponse changeTicket(@PathVariable("id") Long orderId,
                                             @Valid @RequestBody PassengerChangeTicketRequest request) {
        return passengerService.changeTicket(orderId, request);
    }

    @Operation(summary = "Pay my pending ticket change")
    @PostMapping("/changes/{id}/pay")
    public TicketChangeResponse payChange(@PathVariable("id") Long changeId) {
        return passengerService.payChange(changeId);
    }

    @Operation(summary = "Query my ticket changes")
    @GetMapping("/changes")
    public TicketChangePageResponse changes(@RequestParam(value = "status", required = false) String status,
                                            @RequestParam(value = "page", required = false) Integer page,
                                            @RequestParam(value = "size", required = false) Integer size) {
        return passengerService.listChanges(status, page, size);
    }

    @Operation(summary = "Query my ticket change detail")
    @GetMapping("/changes/{id}")
    public TicketChangeResponse changeDetail(@PathVariable("id") Long changeId) {
        return passengerService.changeDetail(changeId);
    }

    @Operation(summary = "Query my transaction status center")
    @GetMapping("/transactions/summary")
    public PassengerTransactionSummaryResponse transactionSummary() {
        return ticketChangeService.passengerTransactionSummary(AuthContext.current().getUserId());
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

    @Operation(summary = "Query my notifications")
    @GetMapping("/notifications")
    public NotificationPageResponse notifications(@RequestParam(value = "status", required = false) String status,
                                                  @RequestParam(value = "page", required = false) Integer page,
                                                  @RequestParam(value = "size", required = false) Integer size) {
        return notificationService.listPassengerNotifications(AuthContext.current().getUserId(), status, page, size);
    }

    @Operation(summary = "Query my notification unread count")
    @GetMapping("/notifications/unread-count")
    public NotificationSummaryResponse notificationUnreadCount() {
        return notificationService.passengerSummary(AuthContext.current().getUserId());
    }

    @Operation(summary = "Mark one notification as read")
    @PostMapping("/notifications/{id}/read")
    public NotificationResponse markNotificationRead(@PathVariable("id") Long id) {
        return notificationService.markAsRead(AuthContext.current().getUserId(), id);
    }

    @Operation(summary = "Mark all notifications as read")
    @PostMapping("/notifications/read-all")
    public NotificationSummaryResponse markAllNotificationsRead() {
        notificationService.markAllAsRead(AuthContext.current().getUserId());
        return notificationService.passengerSummary(AuthContext.current().getUserId());
    }
}
