package com.example.railway.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.domain.OperationLog;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.OperationLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "审计日志", description = "关键业务操作日志查询")
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "查询最近操作日志")
    @RequiredRole({UserRole.ADMIN, UserRole.RISK_OFFICER})
    @GetMapping
    public List<OperationLog> latestLogs() {
        return operationLogService.latest();
    }
}
