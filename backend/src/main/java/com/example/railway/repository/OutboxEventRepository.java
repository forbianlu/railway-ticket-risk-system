package com.example.railway.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long>, JpaSpecificationExecutor<OutboxEvent> {

    Optional<OutboxEvent> findByEventId(String eventId);

    List<OutboxEvent> findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAscIdAsc(
            OutboxEventStatus status,
            LocalDateTime nextRetryAt,
            Pageable pageable
    );

    List<OutboxEvent> findByStatus(OutboxEventStatus status);

    List<OutboxEvent> findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(String aggregateType, String aggregateId);

    List<OutboxEvent> findByStatusAndUpdatedAtBefore(OutboxEventStatus status, LocalDateTime updatedAt);

    @Query("select e from OutboxEvent e " +
            "where lower(e.eventId) like lower(concat('%', :keyword, '%')) " +
            "or lower(e.eventType) like lower(concat('%', :keyword, '%')) " +
            "or lower(e.aggregateId) like lower(concat('%', :keyword, '%')) " +
            "or lower(e.aggregateType) like lower(concat('%', :keyword, '%')) " +
            "or lower(e.payload) like lower(concat('%', :keyword, '%')) " +
            "order by e.createdAt desc")
    List<OutboxEvent> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
