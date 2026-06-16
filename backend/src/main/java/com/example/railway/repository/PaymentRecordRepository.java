package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByStatus(PaymentStatus status);

    List<PaymentRecord> findTop8ByStatusOrderByCreatedAtDesc(PaymentStatus status);

    boolean existsByPaymentNo(String paymentNo);

    @Query("select p from PaymentRecord p " +
            "where lower(p.paymentNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(p.channelPaymentNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(p.orderNo) like lower(concat('%', :keyword, '%')) " +
            "order by p.createdAt desc")
    List<PaymentRecord> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
