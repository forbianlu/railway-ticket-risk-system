package com.example.railway.service.risk;

import java.time.LocalDateTime;

import com.example.railway.domain.TicketOrder;

public class RiskContext {

    private final TicketOrder order;
    private final LocalDateTime evaluatedAt;

    public RiskContext(TicketOrder order, LocalDateTime evaluatedAt) {
        this.order = order;
        this.evaluatedAt = evaluatedAt;
    }

    public TicketOrder getOrder() {
        return order;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
}
