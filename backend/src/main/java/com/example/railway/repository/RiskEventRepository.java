package com.example.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskStatus;

public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    List<RiskEvent> findTop50ByOrderByCreatedAtDesc();

    List<RiskEvent> findTop50ByStatusOrderByCreatedAtDesc(RiskStatus status);

    List<RiskEvent> findTop50BySceneOrderByCreatedAtDesc(RiskScene scene);

    List<RiskEvent> findTop50ByStatusAndSceneOrderByCreatedAtDesc(RiskStatus status, RiskScene scene);

    long countByHandledFalse();

    long countByStatus(RiskStatus status);
}
