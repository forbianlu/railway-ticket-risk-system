package com.example.railway.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PassengerChangeTicketRequest {

    @NotNull
    private Long trainId;

    @NotNull
    private Long inventoryId;

    @NotBlank
    private String requestId;

    private String reason;

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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
