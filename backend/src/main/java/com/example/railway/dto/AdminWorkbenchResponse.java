package com.example.railway.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminWorkbenchResponse {

    private long failedPaymentCount;
    private long pendingRefundCount;
    private long failedRefundCount;
    private long pendingChangeCount;
    private long failedChangeCount;
    private long pendingRiskCount;
    private long failedOutboxCount;
    private long backlogOutboxCount;
    private long unreadNotificationCount;
    private long totalExceptionCount;
    private LocalDateTime latestCreatedAt;
    private List<AdminWorkbenchItemResponse> exceptionItems = new ArrayList<AdminWorkbenchItemResponse>();

    public long getFailedPaymentCount() {
        return failedPaymentCount;
    }

    public void setFailedPaymentCount(long failedPaymentCount) {
        this.failedPaymentCount = failedPaymentCount;
    }

    public long getPendingRefundCount() {
        return pendingRefundCount;
    }

    public void setPendingRefundCount(long pendingRefundCount) {
        this.pendingRefundCount = pendingRefundCount;
    }

    public long getFailedRefundCount() {
        return failedRefundCount;
    }

    public void setFailedRefundCount(long failedRefundCount) {
        this.failedRefundCount = failedRefundCount;
    }

    public long getPendingChangeCount() {
        return pendingChangeCount;
    }

    public void setPendingChangeCount(long pendingChangeCount) {
        this.pendingChangeCount = pendingChangeCount;
    }

    public long getFailedChangeCount() {
        return failedChangeCount;
    }

    public void setFailedChangeCount(long failedChangeCount) {
        this.failedChangeCount = failedChangeCount;
    }

    public long getPendingRiskCount() {
        return pendingRiskCount;
    }

    public void setPendingRiskCount(long pendingRiskCount) {
        this.pendingRiskCount = pendingRiskCount;
    }

    public long getFailedOutboxCount() {
        return failedOutboxCount;
    }

    public void setFailedOutboxCount(long failedOutboxCount) {
        this.failedOutboxCount = failedOutboxCount;
    }

    public long getBacklogOutboxCount() {
        return backlogOutboxCount;
    }

    public void setBacklogOutboxCount(long backlogOutboxCount) {
        this.backlogOutboxCount = backlogOutboxCount;
    }

    public long getUnreadNotificationCount() {
        return unreadNotificationCount;
    }

    public void setUnreadNotificationCount(long unreadNotificationCount) {
        this.unreadNotificationCount = unreadNotificationCount;
    }

    public long getTotalExceptionCount() {
        return totalExceptionCount;
    }

    public void setTotalExceptionCount(long totalExceptionCount) {
        this.totalExceptionCount = totalExceptionCount;
    }

    public LocalDateTime getLatestCreatedAt() {
        return latestCreatedAt;
    }

    public void setLatestCreatedAt(LocalDateTime latestCreatedAt) {
        this.latestCreatedAt = latestCreatedAt;
    }

    public List<AdminWorkbenchItemResponse> getExceptionItems() {
        return exceptionItems;
    }

    public void setExceptionItems(List<AdminWorkbenchItemResponse> exceptionItems) {
        this.exceptionItems = exceptionItems;
    }
}
