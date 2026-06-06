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
    private String passengerIdType;
    private String passengerIdNoMasked;
    private String passengerPhoneMasked;
    private LocalDate travelDate;
    private String seatType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDeadlineAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime closedAt;

    public static OrderResponse from(TicketOrder order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setRequestId(order.getRequestId());
        response.setTrainNo(order.getTrain().getTrainNo());
        response.setPassengerName(order.getPassengerName());
        response.setPassengerIdType(order.getPassengerIdType() == null ? null : order.getPassengerIdType().name());
        response.setPassengerIdNoMasked(order.getPassengerIdNoMasked());
        response.setPassengerPhoneMasked(order.getPassengerPhoneMasked());
        response.setTravelDate(order.getTravelDate());
        response.setSeatType(order.getSeatType());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt());
        response.setPaymentDeadlineAt(order.getPaymentDeadlineAt());
        response.setPaidAt(order.getPaidAt());
        response.setRefundedAt(order.getRefundedAt());
        response.setClosedAt(order.getClosedAt());
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

    public String getPassengerIdType() {
        return passengerIdType;
    }

    public void setPassengerIdType(String passengerIdType) {
        this.passengerIdType = passengerIdType;
    }

    public String getPassengerIdNoMasked() {
        return passengerIdNoMasked;
    }

    public void setPassengerIdNoMasked(String passengerIdNoMasked) {
        this.passengerIdNoMasked = passengerIdNoMasked;
    }

    public String getPassengerPhoneMasked() {
        return passengerPhoneMasked;
    }

    public void setPassengerPhoneMasked(String passengerPhoneMasked) {
        this.passengerPhoneMasked = passengerPhoneMasked;
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

    public LocalDateTime getPaymentDeadlineAt() {
        return paymentDeadlineAt;
    }

    public void setPaymentDeadlineAt(LocalDateTime paymentDeadlineAt) {
        this.paymentDeadlineAt = paymentDeadlineAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
