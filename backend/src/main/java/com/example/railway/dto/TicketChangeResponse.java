package com.example.railway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.railway.domain.TicketChangeRecord;
import com.example.railway.domain.TicketOrder;

public class TicketChangeResponse {

    private Long id;
    private String changeNo;
    private Long userId;
    private Long originalOrderId;
    private String originalOrderNo;
    private Long newOrderId;
    private String newOrderNo;
    private String originalTicketNo;
    private String newTicketNo;
    private String originalTrainNo;
    private String newTrainNo;
    private BigDecimal oldAmount;
    private BigDecimal newAmount;
    private BigDecimal priceDifference;
    private String status;
    private String requestId;
    private String reason;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private OrderResponse originalOrder;
    private OrderResponse newOrder;

    public static TicketChangeResponse from(TicketChangeRecord record) {
        return from(record, null, null);
    }

    public static TicketChangeResponse from(TicketChangeRecord record, TicketOrder originalOrder, TicketOrder newOrder) {
        TicketChangeResponse response = new TicketChangeResponse();
        response.setId(record.getId());
        response.setChangeNo(record.getChangeNo());
        response.setUserId(record.getUserId());
        response.setOriginalOrderId(record.getOriginalOrderId());
        response.setOriginalOrderNo(record.getOriginalOrderNo());
        response.setNewOrderId(record.getNewOrderId());
        response.setNewOrderNo(record.getNewOrderNo());
        response.setOriginalTicketNo(record.getOriginalTicketNo());
        response.setNewTicketNo(record.getNewTicketNo());
        response.setOriginalTrainNo(record.getOriginalTrainNo());
        response.setNewTrainNo(record.getNewTrainNo());
        response.setOldAmount(record.getOldAmount());
        response.setNewAmount(record.getNewAmount());
        response.setPriceDifference(record.getPriceDifference());
        response.setStatus(record.getStatus() == null ? null : record.getStatus().name());
        response.setRequestId(record.getRequestId());
        response.setReason(record.getReason());
        response.setFailureReason(record.getFailureReason());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        response.setCompletedAt(record.getCompletedAt());
        response.setOriginalOrder(originalOrder == null ? null : OrderResponse.from(originalOrder));
        response.setNewOrder(newOrder == null ? null : OrderResponse.from(newOrder));
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getChangeNo() { return changeNo; }
    public void setChangeNo(String changeNo) { this.changeNo = changeNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getOriginalOrderId() { return originalOrderId; }
    public void setOriginalOrderId(Long originalOrderId) { this.originalOrderId = originalOrderId; }
    public String getOriginalOrderNo() { return originalOrderNo; }
    public void setOriginalOrderNo(String originalOrderNo) { this.originalOrderNo = originalOrderNo; }
    public Long getNewOrderId() { return newOrderId; }
    public void setNewOrderId(Long newOrderId) { this.newOrderId = newOrderId; }
    public String getNewOrderNo() { return newOrderNo; }
    public void setNewOrderNo(String newOrderNo) { this.newOrderNo = newOrderNo; }
    public String getOriginalTicketNo() { return originalTicketNo; }
    public void setOriginalTicketNo(String originalTicketNo) { this.originalTicketNo = originalTicketNo; }
    public String getNewTicketNo() { return newTicketNo; }
    public void setNewTicketNo(String newTicketNo) { this.newTicketNo = newTicketNo; }
    public String getOriginalTrainNo() { return originalTrainNo; }
    public void setOriginalTrainNo(String originalTrainNo) { this.originalTrainNo = originalTrainNo; }
    public String getNewTrainNo() { return newTrainNo; }
    public void setNewTrainNo(String newTrainNo) { this.newTrainNo = newTrainNo; }
    public BigDecimal getOldAmount() { return oldAmount; }
    public void setOldAmount(BigDecimal oldAmount) { this.oldAmount = oldAmount; }
    public BigDecimal getNewAmount() { return newAmount; }
    public void setNewAmount(BigDecimal newAmount) { this.newAmount = newAmount; }
    public BigDecimal getPriceDifference() { return priceDifference; }
    public void setPriceDifference(BigDecimal priceDifference) { this.priceDifference = priceDifference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public OrderResponse getOriginalOrder() { return originalOrder; }
    public void setOriginalOrder(OrderResponse originalOrder) { this.originalOrder = originalOrder; }
    public OrderResponse getNewOrder() { return newOrder; }
    public void setNewOrder(OrderResponse newOrder) { this.newOrder = newOrder; }
}
