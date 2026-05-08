package com.example.railway.service.risk;

import com.example.railway.domain.RiskLevel;
import com.example.railway.domain.RiskType;

public class RiskHit {

    private final RiskType riskType;
    private final RiskLevel riskLevel;
    private final String reason;

    public RiskHit(RiskType riskType, RiskLevel riskLevel, String reason) {
        this.riskType = riskType;
        this.riskLevel = riskLevel;
        this.reason = reason;
    }

    public RiskType getRiskType() {
        return riskType;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getReason() {
        return reason;
    }
}
