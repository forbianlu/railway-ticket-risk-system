package com.example.railway.dto;

import java.math.BigDecimal;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class RefundCallbackRequest {

    @NotBlank
    @Size(max = 40)
    private String refundNo;

    @NotBlank
    @Size(max = 64)
    private String callbackRequestId;

    @Size(max = 64)
    private String channelRefundNo;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Boolean success;

    @Size(max = 200)
    private String message;

    @NotBlank
    @Size(max = 32)
    private String timestamp;

    @NotBlank
    @Size(max = 128)
    private String signature;

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public String getCallbackRequestId() {
        return callbackRequestId;
    }

    public void setCallbackRequestId(String callbackRequestId) {
        this.callbackRequestId = callbackRequestId;
    }

    public String getChannelRefundNo() {
        return channelRefundNo;
    }

    public void setChannelRefundNo(String channelRefundNo) {
        this.channelRefundNo = channelRefundNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
