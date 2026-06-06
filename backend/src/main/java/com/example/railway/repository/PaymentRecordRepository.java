package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.PaymentStatus;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long>, JpaSpecificationExecutor<PaymentRecord> {

    Optional<PaymentRecord> findByPaymentNo(String paymentNo);

    Optional<PaymentRecord> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<PaymentRecord> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    Optional<PaymentRecord> findByRequestId(String requestId);

    Optional<PaymentRecord> findByCallbackRequestId(String callbackRequestId);

    List<PaymentRecord> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    long countByUserId(Long userId);

    boolean existsByPaymentNo(String paymentNo);
}
