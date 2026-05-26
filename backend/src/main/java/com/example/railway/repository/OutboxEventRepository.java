package com.example.railway.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>, JpaSpecificationExecutor<OutboxEvent> {

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAscIdAsc(
            OutboxEventStatus status,
            LocalDateTime nextRetryAt,
            Pageable pageable
    );
}
