package com.example.railway.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PaymentCallbackRequest {

    @NotBlank
    @Size(max = 40)
    private String paymentNo;

    @NotBlank
    @Size(max = 64)
    private String callbackRequestId;

    @NotNull
    private Boolean success;

    @Size(max = 200)
    private String message;

    public String getPaymentNo() {
        return paymentNo;
    }

    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    public String getCallbackRequestId() {
        return callbackRequestId;
    }

    public void setCallbackRequestId(String callbackRequestId) {
        this.callbackRequestId = callbackRequestId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
