package com.example.railway.service.outbox;

import java.time.Duration;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;
import com.example.railway.dto.OutboxEventPageResponse;
import com.example.railway.dto.OutboxEventResponse;
import com.example.railway.dto.OutboxEventSummaryResponse;
import com.example.railway.repository.OutboxEventRepository;

@Service
public class OutboxEventQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventQueryService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(readOnly = true)
    public OutboxEventPageResponse listEvents(String status, String eventType, Integer page, Integer size) {
        final OutboxEventStatus eventStatus = parseStatus(status);
        final String normalizedEventType = normalizeText(eventType);
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        Page<OutboxEvent> eventPage = outboxEventRepository.findAll(
                buildSpecification(eventStatus, normalizedEventType),
                pageRequest
        );
        return OutboxEventPageResponse.from(eventPage);
    }

    @Transactional(readOnly = true)
    public OutboxEventSummaryResponse summary() {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        OutboxEventSummaryResponse response = new OutboxEventSummaryResponse();
        Map<String, Long> byType = new LinkedHashMap<String, Long>();
        Map<String, Long> byStatus = new LinkedHashMap<String, Long>();
        Map<String, Long> failedByType = new LinkedHashMap<String, Long>();
        long processSecondsSum = 0L;
        long processSecondsCount = 0L;

        for (OutboxEvent event : events) {
            OutboxEventStatus status = event.getStatus();
            response.setTotalCount(response.getTotalCount() + 1);
            if (status == OutboxEventStatus.PENDING) {
                response.setPendingCount(response.getPendingCount() + 1);
                if (event.getRetryCount() > 0) {
                    response.setRetryingCount(response.getRetryingCount() + 1);
                }
            } else if (status == OutboxEventStatus.PROCESSING) {
                response.setProcessingCount(response.getProcessingCount() + 1);
            } else if (status == OutboxEventStatus.DONE) {
                response.setDoneCount(response.getDoneCount() + 1);
            } else if (status == OutboxEventStatus.FAILED) {
                response.setFailedCount(response.getFailedCount() + 1);
                increment(failedByType, event.getEventType());
            }
            if (event.getRetryCount() >= event.getMaxRetryCount()) {
                response.setMaxRetryReachedCount(response.getMaxRetryReachedCount() + 1);
            }
            increment(byType, event.getEventType());
            increment(byStatus, status == null ? "UNKNOWN" : status.name());
            response.setLatestCreatedAt(max(response.getLatestCreatedAt(), event.getCreatedAt()));
            response.setLatestProcessedAt(max(response.getLatestProcessedAt(), event.getProcessedAt()));
            if (event.getCreatedAt() != null && event.getProcessedAt() != null) {
                processSecondsSum += Math.max(0L, Duration.between(event.getCreatedAt(), event.getProcessedAt()).getSeconds());
                processSecondsCount++;
            }
        }

        response.setEventCountByType(byType);
        response.setEventCountByStatus(byStatus);
        response.setFailedCountByType(failedByType);
        response.setBacklogCount(response.getPendingCount() + response.getProcessingCount());
        response.setFailureRate(response.getFailedCount() / (double) Math.max(1L, response.getTotalCount()));
        response.setAverageProcessSeconds(processSecondsCount == 0 ? 0.0 : processSecondsSum / (double) processSecondsCount);
        return response;
    }

    @Transactional
    public OutboxEventResponse retryFailedEvent(Long id) {
        OutboxEvent event = outboxEventRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Outbox event not found"));
        if (event.getStatus() != OutboxEventStatus.FAILED) {
            throw new BusinessException("Only FAILED outbox events can be retried");
        }
        requeueFailedEvent(event, LocalDateTime.now());
        return OutboxEventResponse.from(outboxEventRepository.save(event));
    }

    @Transactional
    public int retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findByStatus(OutboxEventStatus.FAILED);
        LocalDateTime now = LocalDateTime.now();
        for (OutboxEvent event : failedEvents) {
            requeueFailedEvent(event, now);
        }
        outboxEventRepository.saveAll(failedEvents);
        return failedEvents.size();
    }

    private void requeueFailedEvent(OutboxEvent event, LocalDateTime now) {
        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextRetryAt(now);
        event.setUpdatedAt(now);
        event.setProcessedAt(null);
    }

    private Specification<OutboxEvent> buildSpecification(final OutboxEventStatus status, final String eventType) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (eventType != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), eventType.toUpperCase()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void increment(Map<String, Long> counts, String key) {
        String normalizedKey = key == null ? "UNKNOWN" : key;
        Long current = counts.get(normalizedKey);
        counts.put(normalizedKey, current == null ? 1L : current + 1L);
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        if (right == null) {
            return left;
        }
        if (left == null || right.isAfter(left)) {
            return right;
        }
        return left;
    }

    private OutboxEventStatus parseStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }
        try {
            return OutboxEventStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Outbox 事件状态不合法: " + status);
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new BusinessException("页码不能小于 0");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size <= 0) {
            throw new BusinessException("每页大小必须大于 0");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
