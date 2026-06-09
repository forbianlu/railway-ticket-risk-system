package com.example.railway.dto;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class NotificationSummaryResponse {

    private long totalCount;
    private long unreadCount;
    private long readCount;
    private Map<String, Long> unreadCountByType = new LinkedHashMap<String, Long>();
    private Map<String, Long> totalCountByType = new LinkedHashMap<String, Long>();
    private Map<String, Long> countByType = new LinkedHashMap<String, Long>();
    private Map<String, Long> countByStatus = new LinkedHashMap<String, Long>();
    private LocalDateTime latestCreatedAt;

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getReadCount() {
        return readCount;
    }

    public void setReadCount(long readCount) {
        this.readCount = readCount;
    }

    public Map<String, Long> getUnreadCountByType() {
        return unreadCountByType;
    }

    public void setUnreadCountByType(Map<String, Long> unreadCountByType) {
        this.unreadCountByType = unreadCountByType;
    }

    public Map<String, Long> getTotalCountByType() {
        return totalCountByType;
    }

    public void setTotalCountByType(Map<String, Long> totalCountByType) {
        this.totalCountByType = totalCountByType;
    }

    public Map<String, Long> getCountByType() {
        return countByType;
    }

    public void setCountByType(Map<String, Long> countByType) {
        this.countByType = countByType;
    }

    public Map<String, Long> getCountByStatus() {
        return countByStatus;
    }

    public void setCountByStatus(Map<String, Long> countByStatus) {
        this.countByStatus = countByStatus;
    }

    public LocalDateTime getLatestCreatedAt() {
        return latestCreatedAt;
    }

    public void setLatestCreatedAt(LocalDateTime latestCreatedAt) {
        this.latestCreatedAt = latestCreatedAt;
    }
}
