package com.example.railway.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.railway.domain.NotificationRecord;
import com.example.railway.domain.NotificationStatus;
import com.example.railway.domain.NotificationType;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long>, JpaSpecificationExecutor<NotificationRecord> {

    Optional<NotificationRecord> findByNotificationNo(String notificationNo);

    Optional<NotificationRecord> findByIdAndUserId(Long id, Long userId);

    boolean existsByNotificationNo(String notificationNo);

    boolean existsByUserIdAndTypeAndBusinessTypeAndBusinessId(Long userId,
                                                              NotificationType type,
                                                              String businessType,
                                                              String businessId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    long countByStatus(NotificationStatus status);

    List<NotificationRecord> findByUserId(Long userId);

    List<NotificationRecord> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
