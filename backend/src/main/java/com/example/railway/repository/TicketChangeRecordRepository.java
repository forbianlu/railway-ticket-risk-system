package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.TicketChangeRecord;
import com.example.railway.domain.TicketChangeStatus;

public interface TicketChangeRecordRepository extends JpaRepository<TicketChangeRecord, Long>, JpaSpecificationExecutor<TicketChangeRecord> {

    Optional<TicketChangeRecord> findByChangeNo(String changeNo);

    Optional<TicketChangeRecord> findByIdAndUserId(Long id, Long userId);

    Optional<TicketChangeRecord> findByNewOrderId(Long newOrderId);

    Optional<TicketChangeRecord> findByUserIdAndRequestId(Long userId, String requestId);

    List<TicketChangeRecord> findByOriginalOrderIdOrNewOrderIdOrderByCreatedAtDesc(Long originalOrderId, Long newOrderId);

    Page<TicketChangeRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<TicketChangeRecord> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TicketChangeStatus status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, TicketChangeStatus status);

    long countByStatus(TicketChangeStatus status);

    List<TicketChangeRecord> findTop8ByStatusOrderByCreatedAtDesc(TicketChangeStatus status);

    boolean existsByChangeNo(String changeNo);

    @Query("select c from TicketChangeRecord c " +
            "where lower(c.changeNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.originalOrderNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.newOrderNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.originalTicketNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.newTicketNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.originalTrainNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.newTrainNo) like lower(concat('%', :keyword, '%')) " +
            "order by c.createdAt desc")
    List<TicketChangeRecord> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
