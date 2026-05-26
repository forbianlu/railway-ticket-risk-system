package com.example.railway.domain;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
