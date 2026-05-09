package com.example.railway.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.TicketOrder;

public interface TicketOrderRepository extends JpaRepository<TicketOrder, Long> {

    List<TicketOrder> findTop20ByOrderByCreatedAtDesc();

    List<TicketOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(OrderStatus status);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);

    long countByUserIdAndStatusAndRefundedAtAfter(Long userId, OrderStatus status, LocalDateTime refundedAt);

    long countByInventory_Id(Long inventoryId);

    @Query("select coalesce(sum(o.amount), 0) from TicketOrder o where o.userId = :userId and o.createdAt >= :createdAt and o.status = com.example.railway.domain.OrderStatus.PAID")
    BigDecimal sumPaidAmountByUserAfter(@Param("userId") Long userId, @Param("createdAt") LocalDateTime createdAt);

    @Query("select o.train.trainNo, count(o.id) from TicketOrder o group by o.train.trainNo order by count(o.id) desc")
    List<Object[]> findPopularTrainStats(Pageable pageable);
}
