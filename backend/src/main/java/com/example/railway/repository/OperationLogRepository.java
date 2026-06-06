package com.example.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.OperationLog;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findTop50ByOrderByCreatedAtDesc();

    List<OperationLog> findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);
}
