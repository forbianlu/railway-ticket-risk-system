package com.example.railway.service.outbox;

public final class OutboxEventTypes {

    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_REFUNDED = "ORDER_REFUNDED";
    public static final String ORDER_CLOSED = "ORDER_CLOSED";
    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String REFUND_CREATED = "REFUND_CREATED";
    public static final String REFUND_SUCCEEDED = "REFUND_SUCCEEDED";
    public static final String REFUND_FAILED = "REFUND_FAILED";
    public static final String RISK_EVENT_CREATED = "RISK_EVENT_CREATED";
    public static final String RISK_EVENT_HANDLED = "RISK_EVENT_HANDLED";
    public static final String TICKET_ISSUED = "TICKET_ISSUED";
    public static final String TICKET_REFUNDED = "TICKET_REFUNDED";
    public static final String TICKET_CANCELLED = "TICKET_CANCELLED";
    public static final String NOTIFICATION_CREATED = "NOTIFICATION_CREATED";

    private OutboxEventTypes() {
    }
}
