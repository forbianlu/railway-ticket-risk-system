package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class DashboardSummary {

    private long totalOrders;
    private long paidOrders;
    private long refundedOrders;
    private long totalRiskEvents;
    private long openRiskEvents;
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

    public List<TrainOrderStat> getPopularTrains() {
        return popularTrains;
    }

    public void setPopularTrains(List<TrainOrderStat> popularTrains) {
        this.popularTrains = popularTrains;
    }
}
