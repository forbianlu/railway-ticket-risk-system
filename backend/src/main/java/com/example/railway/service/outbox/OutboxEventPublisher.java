package com.example.railway.service.outbox;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;
import com.example.railway.repository.OutboxEventRepository;

@Service
public class OutboxEventPublisher {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public OutboxEvent publish(String eventType,
                               String aggregateType,
                               String aggregateId,
                               Map<String, Object> payload) {
        return publish(eventType, aggregateType, aggregateId, toJson(payload), DEFAULT_MAX_RETRY_COUNT);
    }

    @Transactional
    public OutboxEvent publish(String eventType,
                               String aggregateType,
                               String aggregateId,
                               String payload,
                               int maxRetryCount) {
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setPayload(payload == null || payload.trim().isEmpty() ? "{}" : payload);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setRetryCount(0);
        event.setMaxRetryCount(Math.max(1, maxRetryCount));
        event.setNextRetryAt(now);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return outboxEventRepository.save(event);
    }

    public String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                builder.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else {
                builder.append('"').append(escape(String.valueOf(value))).append('"');
            }
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private String escape(String value) {
        return String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
