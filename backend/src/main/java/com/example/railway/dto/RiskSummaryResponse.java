package com.example.railway.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class RiskSummaryResponse {

    private long totalRiskCount;
    private long pendingRiskCount;
    private long confirmedRiskCount;
    private long falsePositiveRiskCount;
    private long closedRiskCount;
    private double pendingRate;
    private double confirmedRate;
    private double falsePositiveRate;
    private double closedRate;
    private double handlingCompletionRate;
    private double averageHandleMinutes;
    private Map<String, Long> riskCountByScene = new LinkedHashMap<String, Long>();
    private Map<String, Long> riskCountByStatus = new LinkedHashMap<String, Long>();

    public long getTotalRiskCount() {
        return totalRiskCount;
    }

    public void setTotalRiskCount(long totalRiskCount) {
        this.totalRiskCount = totalRiskCount;
    }

    public long getPendingRiskCount() {
        return pendingRiskCount;
    }

    public void setPendingRiskCount(long pendingRiskCount) {
        this.pendingRiskCount = pendingRiskCount;
    }

    public long getConfirmedRiskCount() {
        return confirmedRiskCount;
    }

    public void setConfirmedRiskCount(long confirmedRiskCount) {
        this.confirmedRiskCount = confirmedRiskCount;
    }

    public long getFalsePositiveRiskCount() {
        return falsePositiveRiskCount;
    }

    public void setFalsePositiveRiskCount(long falsePositiveRiskCount) {
        this.falsePositiveRiskCount = falsePositiveRiskCount;
    }

    public long getClosedRiskCount() {
        return closedRiskCount;
    }

    public void setClosedRiskCount(long closedRiskCount) {
        this.closedRiskCount = closedRiskCount;
    }

    public double getPendingRate() {
        return pendingRate;
    }

    public void setPendingRate(double pendingRate) {
        this.pendingRate = pendingRate;
    }

    public double getConfirmedRate() {
        return confirmedRate;
    }

    public void setConfirmedRate(double confirmedRate) {
        this.confirmedRate = confirmedRate;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public void setFalsePositiveRate(double falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
    }

    public double getClosedRate() {
        return closedRate;
    }

    public void setClosedRate(double closedRate) {
        this.closedRate = closedRate;
    }

    public double getHandlingCompletionRate() {
        return handlingCompletionRate;
    }

    public void setHandlingCompletionRate(double handlingCompletionRate) {
        this.handlingCompletionRate = handlingCompletionRate;
    }

    public double getAverageHandleMinutes() {
        return averageHandleMinutes;
    }

    public void setAverageHandleMinutes(double averageHandleMinutes) {
        this.averageHandleMinutes = averageHandleMinutes;
    }

    public Map<String, Long> getRiskCountByScene() {
        return riskCountByScene;
    }

    public void setRiskCountByScene(Map<String, Long> riskCountByScene) {
        this.riskCountByScene = riskCountByScene;
    }

    public Map<String, Long> getRiskCountByStatus() {
        return riskCountByStatus;
    }

    public void setRiskCountByStatus(Map<String, Long> riskCountByStatus) {
        this.riskCountByStatus = riskCountByStatus;
    }
}
