package com.example.railway.service.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;
import com.example.railway.repository.OutboxEventRepository;

@Service
public class OutboxEventDispatcher {

    private static final int DEFAULT_BATCH_SIZE = 20;
    private static final long RETRY_DELAY_SECONDS = 30L;

    private final OutboxEventRepository outboxEventRepository;
    private final List<OutboxEventHandler> handlers;

    public OutboxEventDispatcher(OutboxEventRepository outboxEventRepository,
                                 List<OutboxEventHandler> handlers) {
        this.outboxEventRepository = outboxEventRepository;
        this.handlers = handlers;
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void scheduledDispatch() {
        dispatchOnce(DEFAULT_BATCH_SIZE);
    }

    @Transactional
    public int dispatchOnce() {
        return dispatchOnce(DEFAULT_BATCH_SIZE);
    }

    @Transactional
    public int dispatchOnce(int batchSize) {
        List<OutboxEvent> events = outboxEventRepository
                .findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAscIdAsc(
                        OutboxEventStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, Math.max(1, batchSize))
                );
        int processed = 0;
        for (OutboxEvent event : events) {
            process(event);
            processed++;
        }
        return processed;
    }

    private void process(OutboxEvent event) {
        LocalDateTime now = LocalDateTime.now();
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setUpdatedAt(now);
        outboxEventRepository.save(event);

        try {
            OutboxEventHandler handler = findHandler(event.getEventType());
            handler.handle(event);
            event.setStatus(OutboxEventStatus.DONE);
            event.setProcessedAt(LocalDateTime.now());
            event.setUpdatedAt(event.getProcessedAt());
            event.setLastError(null);
        } catch (Exception exception) {
            int nextRetryCount = event.getRetryCount() + 1;
            event.setRetryCount(nextRetryCount);
            event.setLastError(truncate(exception.getMessage()));
            event.setUpdatedAt(LocalDateTime.now());
            if (nextRetryCount >= event.getMaxRetryCount()) {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setProcessedAt(event.getUpdatedAt());
            } else {
                event.setStatus(OutboxEventStatus.PENDING);
                event.setNextRetryAt(event.getUpdatedAt().plusSeconds(RETRY_DELAY_SECONDS * nextRetryCount));
            }
        }
        outboxEventRepository.save(event);
    }

    private OutboxEventHandler findHandler(String eventType) {
        for (OutboxEventHandler handler : handlers) {
            if (handler.supports(eventType)) {
                return handler;
            }
        }
        throw new IllegalStateException("No outbox handler for event type " + eventType);
    }

    private String truncate(String message) {
        String normalized = message == null ? "Unknown outbox handler error" : message;
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }
}
