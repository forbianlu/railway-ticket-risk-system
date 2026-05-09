package com.example.railway.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.railway.domain.TicketOrder;

public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long userId;
    private String requestId;
    private String trainNo;
    private String passengerName;
    private LocalDate travelDate;
    private String seatType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime refundedAt;

    public static OrderResponse from(TicketOrder order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setRequestId(order.getRequestId());
        response.setTrainNo(order.getTrain().getTrainNo());
        response.setPassengerName(order.getPassengerName());
        response.setTravelDate(order.getTravelDate());
        response.setSeatType(order.getSeatType());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());
        response.setRefundedAt(order.getRefundedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}
