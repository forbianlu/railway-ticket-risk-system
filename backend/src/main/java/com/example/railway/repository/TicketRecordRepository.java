package com.example.railway.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.TicketRecord;
import com.example.railway.domain.TicketStatus;

public interface TicketRecordRepository extends JpaRepository<TicketRecord, Long> {

    Optional<TicketRecord> findByTicketNo(String ticketNo);

    Optional<TicketRecord> findByOrderId(Long orderId);

    long countByStatus(TicketStatus status);

    Page<TicketRecord> findByUserIdOrderByTravelDateDescCreatedAtDesc(Long userId, Pageable pageable);

    Page<TicketRecord> findByUserIdAndStatusOrderByTravelDateDescCreatedAtDesc(Long userId, TicketStatus status, Pageable pageable);

    @Query("select t from TicketRecord t " +
            "where lower(t.ticketNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(t.orderNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(t.passengerName) like lower(concat('%', :keyword, '%')) " +
            "or lower(t.trainNo) like lower(concat('%', :keyword, '%')) " +
            "order by t.createdAt desc")
    java.util.List<TicketRecord> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
