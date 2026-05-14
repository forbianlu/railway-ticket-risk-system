package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardSummary {

    private long totalOrders;
    private long paidOrders;
    private long refundedOrders;
    private long totalRiskEvents;
    private long openRiskEvents;
    private long totalOrderCount;
    private long pendingPaymentOrderCount;
    private long paidOrderCount;
    private long closedOrderCount;
    private long refundedOrderCount;
    private long unhandledRiskCount;
    private double refundRate;
    private double riskRate;
    private List<TrainOrderStat> popularTrains = new ArrayList<TrainOrderStat>();

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPaidOrders() {
        return paidOrders;
    }

    public void setPaidOrders(long paidOrders) {
        this.paidOrders = paidOrders;
    }

    public long getRefundedOrders() {
        return refundedOrders;
    }

    public void setRefundedOrders(long refundedOrders) {
        this.refundedOrders = refundedOrders;
    }

    public long getTotalRiskEvents() {
        return totalRiskEvents;
    }

    public void setTotalRiskEvents(long totalRiskEvents) {
        this.totalRiskEvents = totalRiskEvents;
    }

    public long getOpenRiskEvents() {
        return openRiskEvents;
    }

    public void setOpenRiskEvents(long openRiskEvents) {
        this.openRiskEvents = openRiskEvents;
    }

    public long getTotalOrderCount() {
        return totalOrderCount;
    }

    public void setTotalOrderCount(long totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }

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

    public long getUnhandledRiskCount() {
        return unhandledRiskCount;
    }

    public void setUnhandledRiskCount(long unhandledRiskCount) {
        this.unhandledRiskCount = unhandledRiskCount;
    }

    public double getRefundRate() {
        return refundRate;
    }

    public void setRefundRate(double refundRate) {
        this.refundRate = refundRate;
    }

    public double getRiskRate() {
        return riskRate;
    }

    public void setRiskRate(double riskRate) {
        this.riskRate = riskRate;
    }

    public List<TrainOrderStat> getPopularTrains() {
        return popularTrains;
    }

    public void setPopularTrains(List<TrainOrderStat> popularTrains) {
        this.popularTrains = popularTrains;
    }
}
