package com.example.railway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.railway.domain.RefundRecord;

public class RefundResponse {

    private Long id;
    private String refundNo;
    private String paymentNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String channel;
    private String channelRefundNo;
    private String requestId;
    private String callbackRequestId;
    private String callbackMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime refundedAt;

    public static RefundResponse from(RefundRecord record) {
        RefundResponse response = new RefundResponse();
        response.setId(record.getId());
        response.setRefundNo(record.getRefundNo());
        response.setPaymentNo(record.getPaymentNo());
        response.setOrderId(record.getOrderId());
        response.setOrderNo(record.getOrderNo());
        response.setUserId(record.getUserId());
        response.setAmount(record.getAmount());
        response.setStatus(record.getStatus().name());
        response.setChannel(record.getChannel());
        response.setChannelRefundNo(record.getChannelRefundNo());
        response.setRequestId(record.getRequestId());
        response.setCallbackRequestId(record.getCallbackRequestId());
        response.setCallbackMessage(record.getCallbackMessage());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        response.setRefundedAt(record.getRefundedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getChannelRefundNo() { return channelRefundNo; }
    public void setChannelRefundNo(String channelRefundNo) { this.channelRefundNo = channelRefundNo; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getCallbackRequestId() { return callbackRequestId; }
    public void setCallbackRequestId(String callbackRequestId) { this.callbackRequestId = callbackRequestId; }
    public String getCallbackMessage() { return callbackMessage; }
    public void setCallbackMessage(String callbackMessage) { this.callbackMessage = callbackMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
}
