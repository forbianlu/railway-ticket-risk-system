package com.example.railway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.TicketRecord;
import com.example.railway.domain.TicketStatus;

public interface TicketRecordRepository extends JpaRepository<TicketRecord, Long> {

    Optional<TicketRecord> findByTicketNo(String ticketNo);

    Optional<TicketRecord> findByOrderId(Long orderId);

    long countByStatus(TicketStatus status);
}
