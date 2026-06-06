package com.example.railway.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.railway.domain.TicketRecord;

public class TicketResponse {

    private Long id;
    private String ticketNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String trainNo;
    private String departureStation;
    private String arrivalStation;
    private LocalDate travelDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private String seatType;
    private String passengerName;
    private String passengerIdCardMasked;
    private BigDecimal amount;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime invalidatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TicketResponse from(TicketRecord record) {
        if (record == null) {
            return null;
        }
        TicketResponse response = new TicketResponse();
        response.setId(record.getId());
        response.setTicketNo(record.getTicketNo());
        response.setOrderId(record.getOrderId());
        response.setOrderNo(record.getOrderNo());
        response.setUserId(record.getUserId());
        response.setTrainNo(record.getTrainNo());
        response.setDepartureStation(record.getDepartureStation());
        response.setArrivalStation(record.getArrivalStation());
        response.setTravelDate(record.getTravelDate());
        response.setDepartureTime(record.getDepartureTime());
        response.setArrivalTime(record.getArrivalTime());
        response.setSeatType(record.getSeatType());
        response.setPassengerName(record.getPassengerName());
        response.setPassengerIdCardMasked(record.getPassengerIdCardMasked());
        response.setAmount(record.getAmount());
        response.setStatus(record.getStatus() == null ? null : record.getStatus().name());
        response.setIssuedAt(record.getIssuedAt());
        response.setInvalidatedAt(record.getInvalidatedAt());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTrainNo() { return trainNo; }
    public void setTrainNo(String trainNo) { this.trainNo = trainNo; }
    public String getDepartureStation() { return departureStation; }
    public void setDepartureStation(String departureStation) { this.departureStation = departureStation; }
    public String getArrivalStation() { return arrivalStation; }
    public void setArrivalStation(String arrivalStation) { this.arrivalStation = arrivalStation; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    public String getPassengerIdCardMasked() { return passengerIdCardMasked; }
    public void setPassengerIdCardMasked(String passengerIdCardMasked) { this.passengerIdCardMasked = passengerIdCardMasked; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getInvalidatedAt() { return invalidatedAt; }
    public void setInvalidatedAt(LocalDateTime invalidatedAt) { this.invalidatedAt = invalidatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
