package com.example.railway.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.OperationLog;
import com.example.railway.service.OperationLogService;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public List<OperationLog> latestLogs() {
        return operationLogService.latest();
    }
}
