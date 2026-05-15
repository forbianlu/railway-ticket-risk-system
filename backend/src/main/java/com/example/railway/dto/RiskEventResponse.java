package com.example.railway.dto;

import java.time.LocalDateTime;

import com.example.railway.domain.RiskEvent;

public class RiskEventResponse {

    private Long id;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String riskType;
    private String riskLevel;
    private String status;
    private String scene;
    private String reason;
    private Boolean handled;
    private String handleRemark;
    private String handledBy;
    private LocalDateTime handledAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;

    public static RiskEventResponse from(RiskEvent riskEvent) {
        RiskEventResponse response = new RiskEventResponse();
        response.setId(riskEvent.getId());
        response.setUserId(riskEvent.getUserId());
        response.setRiskType(riskEvent.getRiskType().name());
        response.setRiskLevel(riskEvent.getRiskLevel().name());
        response.setStatus(riskEvent.getStatus() == null
                ? (Boolean.TRUE.equals(riskEvent.getHandled()) ? "CLOSED" : "PENDING")
                : riskEvent.getStatus().name());
        response.setScene(riskEvent.getScene() == null ? null : riskEvent.getScene().name());
        response.setReason(riskEvent.getReason());
        response.setHandled(riskEvent.getHandled());
        response.setHandleRemark(riskEvent.getHandleRemark());
        response.setHandledBy(riskEvent.getHandledBy());
        response.setHandledAt(riskEvent.getHandledAt());
        response.setClosedAt(riskEvent.getClosedAt());
        response.setCreatedAt(riskEvent.getCreatedAt());
        if (riskEvent.getOrder() != null) {
            response.setOrderId(riskEvent.getOrder().getId());
            response.setOrderNo(riskEvent.getOrder().getOrderNo());
        }
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRiskType() {
        return riskType;
    }

    public void setRiskType(String riskType) {
        this.riskType = riskType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getHandled() {
        return handled;
    }

    public void setHandled(Boolean handled) {
        this.handled = handled;
    }

    public String getHandleRemark() {
        return handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
