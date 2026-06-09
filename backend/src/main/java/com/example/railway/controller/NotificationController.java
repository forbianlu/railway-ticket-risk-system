package com.example.railway.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.NotificationPageResponse;
import com.example.railway.dto.NotificationSummaryResponse;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification Center", description = "Passenger message records and notification statistics")
@RequiredRole({UserRole.ADMIN, UserRole.RISK_OFFICER, UserRole.OPERATOR})
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Query notification records")
    @GetMapping
    public NotificationPageResponse listNotifications(@RequestParam(value = "userId", required = false) Long userId,
                                                      @RequestParam(value = "status", required = false) String status,
                                                      @RequestParam(value = "type", required = false) String type,
                                                      @RequestParam(value = "businessType", required = false) String businessType,
                                                      @RequestParam(value = "orderNo", required = false) String orderNo,
                                                      @RequestParam(value = "fromDate", required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                      @RequestParam(value = "toDate", required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                                      @RequestParam(value = "page", required = false) Integer page,
                                                      @RequestParam(value = "size", required = false) Integer size) {
        return notificationService.adminListNotifications(userId, status, type, businessType, orderNo, fromDate, toDate, page, size);
    }

    @Operation(summary = "Query notification summary")
    @GetMapping("/summary")
    public NotificationSummaryResponse summary() {
        return notificationService.adminSummary();
    }
}
