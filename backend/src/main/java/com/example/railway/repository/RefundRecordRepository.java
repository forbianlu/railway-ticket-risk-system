package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.RefundStatus;

public interface RefundRecordRepository extends JpaRepository<RefundRecord, Long>, JpaSpecificationExecutor<RefundRecord> {

    Optional<RefundRecord> findByRefundNo(String refundNo);

    Optional<RefundRecord> findByOrderIdAndStatus(Long orderId, RefundStatus status);

    Optional<RefundRecord> findByPaymentNo(String paymentNo);

    Optional<RefundRecord> findByRequestId(String requestId);

    Optional<RefundRecord> findByCallbackRequestId(String callbackRequestId);

    List<RefundRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, RefundStatus status);

    long countByStatus(RefundStatus status);

    List<RefundRecord> findTop8ByStatusOrderByCreatedAtDesc(RefundStatus status);

    boolean existsByRefundNo(String refundNo);

    @Query("select r from RefundRecord r " +
            "where lower(r.refundNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(r.channelRefundNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(r.paymentNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(r.orderNo) like lower(concat('%', :keyword, '%')) " +
            "order by r.createdAt desc")
    List<RefundRecord> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
