package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class PassengerTransactionSummaryResponse {

    private long pendingPaymentOrderCount;
    private long activeTicketCount;
    private long pendingChangeCount;
    private long refundedTicketCount;
    private long pendingRefundCount;
    private long unreadNotificationCount;
    private List<OrderResponse> latestOrders = new ArrayList<OrderResponse>();
    private List<TicketChangeResponse> latestChanges = new ArrayList<TicketChangeResponse>();

    public long getPendingPaymentOrderCount() { return pendingPaymentOrderCount; }
    public void setPendingPaymentOrderCount(long pendingPaymentOrderCount) { this.pendingPaymentOrderCount = pendingPaymentOrderCount; }
    public long getActiveTicketCount() { return activeTicketCount; }
    public void setActiveTicketCount(long activeTicketCount) { this.activeTicketCount = activeTicketCount; }
    public long getPendingChangeCount() { return pendingChangeCount; }
    public void setPendingChangeCount(long pendingChangeCount) { this.pendingChangeCount = pendingChangeCount; }
    public long getRefundedTicketCount() { return refundedTicketCount; }
    public void setRefundedTicketCount(long refundedTicketCount) { this.refundedTicketCount = refundedTicketCount; }
    public long getPendingRefundCount() { return pendingRefundCount; }
    public void setPendingRefundCount(long pendingRefundCount) { this.pendingRefundCount = pendingRefundCount; }
    public long getUnreadNotificationCount() { return unreadNotificationCount; }
    public void setUnreadNotificationCount(long unreadNotificationCount) { this.unreadNotificationCount = unreadNotificationCount; }
    public List<OrderResponse> getLatestOrders() { return latestOrders; }
    public void setLatestOrders(List<OrderResponse> latestOrders) { this.latestOrders = latestOrders; }
    public List<TicketChangeResponse> getLatestChanges() { return latestChanges; }
    public void setLatestChanges(List<TicketChangeResponse> latestChanges) { this.latestChanges = latestChanges; }
}
