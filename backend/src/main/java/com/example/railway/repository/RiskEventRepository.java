package com.example.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.RiskEvent;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    List<RiskEvent> findTop50ByOrderByCreatedAtDesc();

    long countByHandledFalse();
}
