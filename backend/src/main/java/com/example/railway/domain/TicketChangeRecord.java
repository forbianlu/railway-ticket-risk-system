package com.example.railway.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "ticket_change_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ticket_change_no", columnNames = "change_no"),
        @UniqueConstraint(name = "uk_ticket_change_user_request", columnNames = {"user_id", "request_id"})
})
public class TicketChangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_no", nullable = false, length = 48)
    private String changeNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_order_id", nullable = false)
    private Long originalOrderId;

    @Column(name = "original_order_no", nullable = false, length = 64)
    private String originalOrderNo;

    @Column(name = "new_order_id", nullable = false)
    private Long newOrderId;

    @Column(name = "new_order_no", nullable = false, length = 64)
    private String newOrderNo;

    @Column(name = "original_ticket_no", length = 64)
    private String originalTicketNo;

    @Column(name = "new_ticket_no", length = 64)
    private String newTicketNo;

    @Column(name = "original_train_no", nullable = false, length = 32)
    private String originalTrainNo;

    @Column(name = "new_train_no", nullable = false, length = 32)
    private String newTrainNo;

    @Column(name = "old_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal oldAmount;

    @Column(name = "new_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "price_difference", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDifference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketChangeStatus status;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(length = 200)
    private String reason;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    public Long getId() { return id; }
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
    public TicketChangeStatus getStatus() { return status; }
    public void setStatus(TicketChangeStatus status) { this.status = status; }
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
}
