package com.example.railway.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateOrderRequest {

    @NotNull
    private Long userId;

    @Size(max = 64)
    private String requestId;

    @NotNull
    private Long trainId;

    @NotNull
    private Long inventoryId;

    @NotBlank
    @Size(max = 64)
    private String passengerName;

    @NotBlank
    @Size(max = 32)
    private String passengerIdCard;

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

    public Long getTrainId() {
        return trainId;
    }

    public void setTrainId(Long trainId) {
        this.trainId = trainId;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPassengerIdCard() {
        return passengerIdCard;
    }

    public void setPassengerIdCard(String passengerIdCard) {
        this.passengerIdCard = passengerIdCard;
    }
}
