package com.example.railway.controller;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.RiskEventHandleRecordResponse;
import com.example.railway.dto.RiskEventPageResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.RiskSummaryResponse;
import com.example.railway.dto.RiskHandleRequest;
import com.example.railway.security.AuthContext;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.RateLimitService;
import com.example.railway.service.RiskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "风险管理", description = "风险事件查询、统计、处置和处置历史")
@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService riskService;
    private final RateLimitService rateLimitService;

    public RiskController(RiskService riskService,
                          RateLimitService rateLimitService) {
        this.riskService = riskService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "分页筛选风险事件")
    @GetMapping
    public RiskEventPageResponse listRisks(@RequestParam(value = "status", required = false) String status,
                                           @RequestParam(value = "scene", required = false) String scene,
                                           @RequestParam(value = "userId", required = false) Long userId,
                                           @RequestParam(value = "orderNo", required = false) String orderNo,
                                           @RequestParam(value = "fromDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                           @RequestParam(value = "toDate", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                           @RequestParam(value = "page", required = false) Integer page,
                                           @RequestParam(value = "size", required = false) Integer size) {
        return riskService.listRisks(status, scene, userId, orderNo, fromDate, toDate, page, size);
    }

    @Operation(summary = "查询风险运营统计")
    @GetMapping("/summary")
    public RiskSummaryResponse summary() {
        return riskService.summary();
    }

    @Operation(summary = "处置风险事件")
    @RequiredRole({UserRole.RISK_OFFICER, UserRole.ADMIN})
    @PostMapping("/{id}/handle")
    public RiskEventResponse handleRisk(@PathVariable("id") Long riskId,
                                        @Valid @RequestBody(required = false) RiskHandleRequest request,
                                        @RequestParam(value = "operator", required = false) String operator) {
        String currentOperator = operator == null || operator.trim().isEmpty() ? AuthContext.current().getUsername() : operator;
        rateLimitService.check("risk-handle", "rate:risk:handle:user:" + currentOperator);
        return riskService.handleRisk(riskId, request, currentOperator);
    }

    @Operation(summary = "查询风险处置历史")
    @GetMapping("/{id}/handle-records")
    public List<RiskEventHandleRecordResponse> handleRecords(@PathVariable("id") Long riskId) {
        return riskService.handleRecords(riskId);
    }
}
