package com.example.railway.dto;

import java.time.LocalDateTime;

public class PassengerTodoItemResponse {

    private String type;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String actionTarget;
    private Long orderId;
    private Long changeId;
    private LocalDateTime createdAt;

    public static PassengerTodoItemResponse of(String type,
                                               String title,
                                               String description,
                                               String status,
                                               String priority,
                                               String actionTarget,
                                               Long orderId,
                                               Long changeId,
                                               LocalDateTime createdAt) {
        PassengerTodoItemResponse response = new PassengerTodoItemResponse();
        response.setType(type);
        response.setTitle(title);
        response.setDescription(description);
        response.setStatus(status);
        response.setPriority(priority);
        response.setActionTarget(actionTarget);
        response.setOrderId(orderId);
        response.setChangeId(changeId);
        response.setCreatedAt(createdAt);
        return response;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getActionTarget() { return actionTarget; }
    public void setActionTarget(String actionTarget) { this.actionTarget = actionTarget; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
