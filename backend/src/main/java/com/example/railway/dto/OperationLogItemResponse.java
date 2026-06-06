package com.example.railway.dto;

import java.time.LocalDateTime;

import com.example.railway.domain.OperationLog;

public class OperationLogItemResponse {

    private Long id;
    private String operator;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private LocalDateTime createdAt;

    public static OperationLogItemResponse from(OperationLog log) {
        OperationLogItemResponse response = new OperationLogItemResponse();
        response.setId(log.getId());
        response.setOperator(log.getOperator());
        response.setAction(log.getAction());
        response.setTargetType(log.getTargetType());
        response.setTargetId(log.getTargetId());
        response.setDetail(log.getDetail());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
