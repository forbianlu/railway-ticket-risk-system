package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

    boolean existsByRefundNo(String refundNo);
}
