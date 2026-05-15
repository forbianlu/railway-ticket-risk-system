package com.example.railway.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "risk_event_handle_records")
public class RiskEventHandleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long riskEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskStatus toStatus;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false, length = 64)
    private String operatorName;

    @Column(nullable = false)
    private LocalDateTime operatedAt;

    public Long getId() {
        return id;
    }

    public Long getRiskEventId() {
        return riskEventId;
    }

    public void setRiskEventId(Long riskEventId) {
        this.riskEventId = riskEventId;
    }

    public RiskStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(RiskStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public RiskStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(RiskStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(LocalDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }
}
