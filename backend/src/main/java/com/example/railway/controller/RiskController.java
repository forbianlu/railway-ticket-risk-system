package com.example.railway.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.RiskEventHandleRecordResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.RiskHandleRequest;
import com.example.railway.security.AuthContext;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.RiskService;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping
    public List<RiskEventResponse> listRisks(@RequestParam(value = "status", required = false) String status,
                                             @RequestParam(value = "scene", required = false) String scene) {
        return riskService.latestRisks(status, scene);
    }

    @RequiredRole({UserRole.RISK_OFFICER, UserRole.ADMIN})
    @PostMapping("/{id}/handle")
    public RiskEventResponse handleRisk(@PathVariable("id") Long riskId,
                                        @Valid @RequestBody(required = false) RiskHandleRequest request,
                                        @RequestParam(value = "operator", required = false) String operator) {
        String currentOperator = operator == null || operator.trim().isEmpty() ? AuthContext.current().getUsername() : operator;
        return riskService.handleRisk(riskId, request, currentOperator);
    }

    @GetMapping("/{id}/handle-records")
    public List<RiskEventHandleRecordResponse> handleRecords(@PathVariable("id") Long riskId) {
        return riskService.handleRecords(riskId);
    }
}
