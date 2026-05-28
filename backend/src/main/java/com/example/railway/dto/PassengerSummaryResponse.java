package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class PassengerSummaryResponse {

    private long pendingPaymentOrderCount;
    private long paidOrderCount;
    private long closedOrderCount;
    private long refundedOrderCount;
    private long paymentCount;
    private long refundCount;
    private List<OrderResponse> latestOrders = new ArrayList<OrderResponse>();
    private List<OrderResponse> upcomingTrips = new ArrayList<OrderResponse>();

    public long getPendingPaymentOrderCount() {
        return pendingPaymentOrderCount;
    }

    public void setPendingPaymentOrderCount(long pendingPaymentOrderCount) {
        this.pendingPaymentOrderCount = pendingPaymentOrderCount;
    }

    public long getPaidOrderCount() {
        return paidOrderCount;
    }

    public void setPaidOrderCount(long paidOrderCount) {
        this.paidOrderCount = paidOrderCount;
    }

    public long getClosedOrderCount() {
        return closedOrderCount;
    }

    public void setClosedOrderCount(long closedOrderCount) {
        this.closedOrderCount = closedOrderCount;
    }

    public long getRefundedOrderCount() {
        return refundedOrderCount;
    }

    public void setRefundedOrderCount(long refundedOrderCount) {
        this.refundedOrderCount = refundedOrderCount;
    }

    public long getPaymentCount() {
        return paymentCount;
    }

    public void setPaymentCount(long paymentCount) {
        this.paymentCount = paymentCount;
    }

    public long getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(long refundCount) {
        this.refundCount = refundCount;
    }

    public List<OrderResponse> getLatestOrders() {
        return latestOrders;
    }

    public void setLatestOrders(List<OrderResponse> latestOrders) {
        this.latestOrders = latestOrders;
    }

    public List<OrderResponse> getUpcomingTrips() {
        return upcomingTrips;
    }

    public void setUpcomingTrips(List<OrderResponse> upcomingTrips) {
        this.upcomingTrips = upcomingTrips;
    }
}
