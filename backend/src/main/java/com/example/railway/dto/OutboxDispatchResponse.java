package com.example.railway.dto;

public class OutboxDispatchResponse {

    private int processedCount;

    public OutboxDispatchResponse() {
    }

    public OutboxDispatchResponse(int processedCount) {
        this.processedCount = processedCount;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }
}
