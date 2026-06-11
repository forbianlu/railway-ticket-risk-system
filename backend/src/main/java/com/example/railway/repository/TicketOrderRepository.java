package com.example.railway.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.TicketOrder;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long>, JpaSpecificationExecutor<TicketOrder> {

    List<TicketOrder> findTop20ByOrderByCreatedAtDesc();

    List<TicketOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TicketOrder> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<TicketOrder> findTop5ByUserIdAndStatusAndTravelDateGreaterThanEqualOrderByTravelDateAscCreatedAtDesc(Long userId, OrderStatus status, java.time.LocalDate travelDate);

    Optional<TicketOrder> findByIdAndUserId(Long id, Long userId);

    Optional<TicketOrder> findByUserIdAndRequestId(Long userId, String requestId);

    long countByStatus(OrderStatus status);

    long countByUserIdAndStatus(Long userId, OrderStatus status);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);

    long countByUserIdAndStatusAndRefundedAtAfter(Long userId, OrderStatus status, LocalDateTime refundedAt);

    long countByUserIdAndStatusAndPaidAtAfter(Long userId, OrderStatus status, LocalDateTime paidAt);

    long countByInventory_Id(Long inventoryId);

    @Query("select coalesce(sum(o.amount), 0) from TicketOrder o where o.userId = :userId and o.createdAt >= :createdAt and o.status = com.example.railway.domain.OrderStatus.PAID")
    BigDecimal sumPaidAmountByUserAfter(@Param("userId") Long userId, @Param("createdAt") LocalDateTime createdAt);

    List<TicketOrder> findByStatusAndPaymentDeadlineAtBefore(OrderStatus status, LocalDateTime deadline);

    @Query("select o.train.trainNo, count(o.id) from TicketOrder o where o.status in (com.example.railway.domain.OrderStatus.PAID, com.example.railway.domain.OrderStatus.REFUNDED) group by o.train.trainNo order by count(o.id) desc")
    List<Object[]> findPopularTrainStats(Pageable pageable);

    @Query("select o from TicketOrder o " +
            "join o.train t " +
            "join t.departureStation ds " +
            "join t.arrivalStation asn " +
            "where lower(o.orderNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(o.passengerName) like lower(concat('%', :keyword, '%')) " +
            "or lower(t.trainNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(ds.name) like lower(concat('%', :keyword, '%')) " +
            "or lower(asn.name) like lower(concat('%', :keyword, '%')) " +
            "or lower(ds.city) like lower(concat('%', :keyword, '%')) " +
            "or lower(asn.city) like lower(concat('%', :keyword, '%')) " +
            "order by o.createdAt desc")
    List<TicketOrder> searchAdmin(@Param("keyword") String keyword, Pageable pageable);

    List<TicketOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
