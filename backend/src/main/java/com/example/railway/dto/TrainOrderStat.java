package com.example.railway.dto;

public class TrainOrderStat {

    private String trainNo;
    private long orderCount;

    public TrainOrderStat() {
    }

    public TrainOrderStat(String trainNo, long orderCount) {
        this.trainNo = trainNo;
        this.orderCount = orderCount;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }
}
