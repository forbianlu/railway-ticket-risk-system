package com.example.railway.dto;

import java.time.LocalDateTime;

import com.example.railway.domain.OutboxEvent;

public class OutboxEventResponse {

    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String payload;
    private String status;
    private int retryCount;
    private int maxRetryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;

    public static OutboxEventResponse from(OutboxEvent event) {
        OutboxEventResponse response = new OutboxEventResponse();
        response.setId(event.getId());
        response.setEventId(event.getEventId());
        response.setEventType(event.getEventType());
        response.setAggregateType(event.getAggregateType());
        response.setAggregateId(event.getAggregateId());
        response.setPayload(event.getPayload());
        response.setStatus(event.getStatus() == null ? null : event.getStatus().name());
        response.setRetryCount(event.getRetryCount());
        response.setMaxRetryCount(event.getMaxRetryCount());
        response.setNextRetryAt(event.getNextRetryAt());
        response.setLastError(event.getLastError());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setProcessedAt(event.getProcessedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
