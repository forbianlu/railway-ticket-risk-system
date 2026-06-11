package com.example.railway.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.OperationLog;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findTop50ByOrderByCreatedAtDesc();

    List<OperationLog> findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, String targetId);

    @Query("select l from OperationLog l " +
            "where lower(l.operator) like lower(concat('%', :keyword, '%')) " +
            "or lower(l.action) like lower(concat('%', :keyword, '%')) " +
            "or lower(l.targetType) like lower(concat('%', :keyword, '%')) " +
            "or lower(l.targetId) like lower(concat('%', :keyword, '%')) " +
            "or lower(l.detail) like lower(concat('%', :keyword, '%')) " +
            "order by l.createdAt desc")
    List<OperationLog> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
