package com.example.railway.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Train;

public class TrainSearchResponse {

    private Long trainId;
    private String trainNo;
    private String departureStation;
    private String arrivalStation;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Long inventoryId;
    private LocalDate travelDate;
    private String seatType;
    private Integer remainingSeats;
    private BigDecimal price;

    public static TrainSearchResponse from(SeatInventory inventory) {
        Train train = inventory.getTrain();
        TrainSearchResponse response = new TrainSearchResponse();
        response.setTrainId(train.getId());
        response.setTrainNo(train.getTrainNo());
        response.setDepartureStation(train.getDepartureStation().getName());
        response.setArrivalStation(train.getArrivalStation().getName());
        response.setDepartureTime(train.getDepartureTime());
        response.setArrivalTime(train.getArrivalTime());
        response.setInventoryId(inventory.getId());
        response.setTravelDate(inventory.getTravelDate());
        response.setSeatType(inventory.getSeatType());
        response.setRemainingSeats(inventory.getRemainingSeats());
        response.setPrice(inventory.getPrice());
        return response;
    }

    public TrainSearchResponse copy() {
        TrainSearchResponse response = new TrainSearchResponse();
        response.setTrainId(trainId);
        response.setTrainNo(trainNo);
        response.setDepartureStation(departureStation);
        response.setArrivalStation(arrivalStation);
        response.setDepartureTime(departureTime);
        response.setArrivalTime(arrivalTime);
        response.setInventoryId(inventoryId);
        response.setTravelDate(travelDate);
        response.setSeatType(seatType);
        response.setRemainingSeats(remainingSeats);
        response.setPrice(price);
        return response;
    }

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public void setTrainNo(String trainNo) {
        this.trainNo = trainNo;
    }

    public String getDepartureStation() {
        return departureStation;
    }

    public void setDepartureStation(String departureStation) {
        this.departureStation = departureStation;
    }

    public String getArrivalStation() {
        return arrivalStation;
    }

    public void setArrivalStation(String arrivalStation) {
        this.arrivalStation = arrivalStation;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
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

    public Integer getRemainingSeats() {
        return remainingSeats;
    }

    public void setRemainingSeats(Integer remainingSeats) {
        this.remainingSeats = remainingSeats;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
