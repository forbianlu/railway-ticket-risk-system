package com.example.railway.service.risk;

import java.util.Optional;

import com.example.railway.domain.RiskScene;

public interface RiskRule {

    RiskScene scene();

    Optional<RiskHit> evaluate(RiskContext context);
}
