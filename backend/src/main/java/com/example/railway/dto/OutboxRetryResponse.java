package com.example.railway.dto;

public class OutboxRetryResponse {

    private int enqueuedCount;

    public OutboxRetryResponse() {
    }

    public OutboxRetryResponse(int enqueuedCount) {
        this.enqueuedCount = enqueuedCount;
    }

    public int getEnqueuedCount() {
        return enqueuedCount;
    }

    public void setEnqueuedCount(int enqueuedCount) {
        this.enqueuedCount = enqueuedCount;
    }
}
