package com.example.railway.service.outbox;

import org.springframework.stereotype.Component;

import com.example.railway.domain.OutboxEvent;
import com.example.railway.service.OperationLogService;

@Component
public class OperationLogEventHandler implements OutboxEventHandler {

    private final OperationLogService operationLogService;

    public OperationLogEventHandler(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public boolean supports(String eventType) {
        return OutboxEventTypes.ORDER_PAID.equals(eventType)
                || OutboxEventTypes.ORDER_REFUNDED.equals(eventType)
                || OutboxEventTypes.ORDER_CLOSED.equals(eventType)
                || OutboxEventTypes.PAYMENT_SUCCEEDED.equals(eventType)
                || OutboxEventTypes.PAYMENT_FAILED.equals(eventType)
                || OutboxEventTypes.REFUND_CREATED.equals(eventType)
                || OutboxEventTypes.REFUND_SUCCEEDED.equals(eventType)
                || OutboxEventTypes.REFUND_FAILED.equals(eventType)
                || OutboxEventTypes.RISK_EVENT_CREATED.equals(eventType)
                || OutboxEventTypes.RISK_EVENT_HANDLED.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        operationLogService.record(
                "OUTBOX_DISPATCHER",
                "OUTBOX_EVENT_DONE",
                event.getAggregateType(),
                event.getAggregateId(),
                "事件 " + event.getEventType() + " 已处理，eventId=" + event.getEventId()
        );
    }
}
