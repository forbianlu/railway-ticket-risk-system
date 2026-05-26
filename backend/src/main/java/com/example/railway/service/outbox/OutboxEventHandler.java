package com.example.railway.service.outbox;

import com.example.railway.domain.OutboxEvent;

public interface OutboxEventHandler {

    boolean supports(String eventType);

    void handle(OutboxEvent event);
}
