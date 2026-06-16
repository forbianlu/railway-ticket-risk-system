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
    private String actionType;
    private String actionLabel;
    private String actionTarget;
    private String actionHint;
    private String priority;
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
        response.applyAction(record);
        response.setReadAt(record.getReadAt());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private void applyAction(NotificationRecord record) {
        String type = record.getType() == null ? null : record.getType().name();
        if ("ORDER_CREATED".equals(type)) {
            setAction("ORDER_PAYMENT", "Go to payment", "ORDERS", "This order is waiting for payment.", "HIGH");
        } else if ("PAYMENT_SUCCEEDED".equals(type)) {
            setAction("ORDER_DETAIL", "View order", "ORDER_DETAIL", "Payment succeeded. Check the order and ticket timeline.", "NORMAL");
        } else if ("TICKET_ISSUED".equals(type)) {
            setAction("TICKET_WALLET", "View ticket", "TICKETS", "The e-ticket is ready in your ticket wallet.", "NORMAL");
        } else if ("ORDER_CLOSED".equals(type)) {
            setAction("ORDER_DETAIL", "View order", "ORDER_DETAIL", "The order has been closed.", "NORMAL");
        } else if ("ORDER_REFUNDED".equals(type)) {
            setAction("REFUND_RECORDS", "View refund", "REFUNDS", "Refund processing has started.", "HIGH");
        } else if ("REFUND_SUCCEEDED".equals(type)) {
            setAction("REFUND_RECORDS", "View refund", "REFUNDS", "Refund succeeded. Check the refund record.", "NORMAL");
        } else if ("REFUND_FAILED".equals(type)) {
            setAction("REFUND_RECORDS", "Check refund", "REFUNDS", "Refund failed and may need follow-up handling.", "HIGH");
        } else if ("TICKET_CHANGE_PENDING_PAYMENT".equals(type)) {
            setAction("CHANGE_PAYMENT", "Pay change", "CHANGES", "The ticket change requires price-difference payment.", "HIGH");
        } else if ("TICKET_CHANGE_CREATED".equals(type)) {
            setAction("CHANGE_DETAIL", "View change", "CHANGES", "The ticket change request has been submitted.", "NORMAL");
        } else if ("TICKET_CHANGE_SUCCEEDED".equals(type)) {
            setAction("CHANGE_DETAIL", "View change", "CHANGES", "Ticket change completed. Check the new ticket.", "NORMAL");
        } else if ("TICKET_CHANGE_FAILED".equals(type)) {
            setAction("CHANGE_DETAIL", "Check change", "CHANGES", "Ticket change failed. Review the change record.", "HIGH");
        } else if ("RISK_ALERT".equals(type)) {
            setAction("ORDER_DETAIL", "View order", "ORDER_DETAIL", "This transaction has a risk reminder.", "HIGH");
        } else {
            setAction("VIEW_MESSAGE", "View message", "NOTIFICATIONS", "Review this message detail.", "NORMAL");
        }
    }

    private void setAction(String actionType, String actionLabel, String actionTarget, String actionHint, String priority) {
        this.actionType = actionType;
        this.actionLabel = actionLabel;
        this.actionTarget = actionTarget;
        this.actionHint = actionHint;
        this.priority = priority;
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

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getActionTarget() {
        return actionTarget;
    }

    public void setActionTarget(String actionTarget) {
        this.actionTarget = actionTarget;
    }

    public String getActionHint() {
        return actionHint;
    }

    public void setActionHint(String actionHint) {
        this.actionHint = actionHint;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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
