package com.example.railway.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreatePaymentRequest {

    @NotNull
    private Long orderId;

    @Size(max = 64)
    private String requestId;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
