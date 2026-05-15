package com.example.railway.dto;

import java.time.LocalDateTime;

import com.example.railway.domain.RiskEventHandleRecord;

public class RiskEventHandleRecordResponse {

    private Long id;
    private Long riskEventId;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private String operatorName;
    private LocalDateTime operatedAt;

    public static RiskEventHandleRecordResponse from(RiskEventHandleRecord record) {
        RiskEventHandleRecordResponse response = new RiskEventHandleRecordResponse();
        response.setId(record.getId());
        response.setRiskEventId(record.getRiskEventId());
        response.setFromStatus(record.getFromStatus().name());
        response.setToStatus(record.getToStatus().name());
        response.setRemark(record.getRemark());
        response.setOperatorName(record.getOperatorName());
        response.setOperatedAt(record.getOperatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRiskEventId() {
        return riskEventId;
    }

    public void setRiskEventId(Long riskEventId) {
        this.riskEventId = riskEventId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
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
