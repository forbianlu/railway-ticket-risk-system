package com.example.railway.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class OutboxEventSummaryResponse {

    private long totalCount;
    private long pendingCount;
    private long processingCount;
    private long doneCount;
    private long failedCount;
    private long retryingCount;
    private long maxRetryReachedCount;
    private LocalDateTime latestCreatedAt;
    private LocalDateTime latestProcessedAt;
    private Map<String, Long> eventCountByType = new LinkedHashMap<String, Long>();
    private Map<String, Long> eventCountByStatus = new LinkedHashMap<String, Long>();
    private Map<String, Long> failedCountByType = new LinkedHashMap<String, Long>();
    private double averageProcessSeconds;
    private long backlogCount;
    private double failureRate;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    public long getProcessingCount() { return processingCount; }
    public void setProcessingCount(long processingCount) { this.processingCount = processingCount; }
    public long getDoneCount() { return doneCount; }
    public void setDoneCount(long doneCount) { this.doneCount = doneCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    public long getRetryingCount() { return retryingCount; }
    public void setRetryingCount(long retryingCount) { this.retryingCount = retryingCount; }
    public long getMaxRetryReachedCount() { return maxRetryReachedCount; }
    public void setMaxRetryReachedCount(long maxRetryReachedCount) { this.maxRetryReachedCount = maxRetryReachedCount; }
    public LocalDateTime getLatestCreatedAt() { return latestCreatedAt; }
    public void setLatestCreatedAt(LocalDateTime latestCreatedAt) { this.latestCreatedAt = latestCreatedAt; }
    public LocalDateTime getLatestProcessedAt() { return latestProcessedAt; }
    public void setLatestProcessedAt(LocalDateTime latestProcessedAt) { this.latestProcessedAt = latestProcessedAt; }
    public Map<String, Long> getEventCountByType() { return eventCountByType; }
    public void setEventCountByType(Map<String, Long> eventCountByType) { this.eventCountByType = eventCountByType; }
    public Map<String, Long> getEventCountByStatus() { return eventCountByStatus; }
    public void setEventCountByStatus(Map<String, Long> eventCountByStatus) { this.eventCountByStatus = eventCountByStatus; }
    public Map<String, Long> getFailedCountByType() { return failedCountByType; }
    public void setFailedCountByType(Map<String, Long> failedCountByType) { this.failedCountByType = failedCountByType; }
    public double getAverageProcessSeconds() { return averageProcessSeconds; }
    public void setAverageProcessSeconds(double averageProcessSeconds) { this.averageProcessSeconds = averageProcessSeconds; }
    public long getBacklogCount() { return backlogCount; }
    public void setBacklogCount(long backlogCount) { this.backlogCount = backlogCount; }
    public double getFailureRate() { return failureRate; }
    public void setFailureRate(double failureRate) { this.failureRate = failureRate; }
}
