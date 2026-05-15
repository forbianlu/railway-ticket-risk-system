package com.example.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.RiskEventHandleRecord;

public interface RiskEventHandleRecordRepository extends JpaRepository<RiskEventHandleRecord, Long> {

    List<RiskEventHandleRecord> findByRiskEventIdOrderByOperatedAtAsc(Long riskEventId);
}
