package com.example.railway.dto;

import java.time.LocalDateTime;

import com.example.railway.domain.NotificationRecord;

public class NotificationResponse {

    private Long id;
    private String notificationNo;
    private Long userId;
    private String title;
    private String content;
    private String type;
    private String status;
    private String businessType;
    private String businessId;
    private Long orderId;
    private String orderNo;
    private String ticketNo;
    private String paymentNo;
    private String refundNo;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponse from(NotificationRecord record) {
        NotificationResponse response = new NotificationResponse();
        response.setId(record.getId());
        response.setNotificationNo(record.getNotificationNo());
        response.setUserId(record.getUserId());
        response.setTitle(record.getTitle());
        response.setContent(record.getContent());
        response.setType(record.getType() == null ? null : record.getType().name());
        response.setStatus(record.getStatus() == null ? null : record.getStatus().name());
        response.setBusinessType(record.getBusinessType());
        response.setBusinessId(record.getBusinessId());
        response.setOrderId(record.getOrderId());
        response.setOrderNo(record.getOrderNo());
        response.setTicketNo(record.getTicketNo());
        response.setPaymentNo(record.getPaymentNo());
        response.setRefundNo(record.getRefundNo());
        response.setReadAt(record.getReadAt());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNotificationNo() {
        return notificationNo;
    }

    public void setNotificationNo(String notificationNo) {
        this.notificationNo = notificationNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
