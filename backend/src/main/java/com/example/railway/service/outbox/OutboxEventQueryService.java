package com.example.railway.service.outbox;

import java.util.ArrayList;
import java.util.List;

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
