package com.example.railway.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "健康检查", description = "服务健康状态")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Operation(summary = "查询服务健康状态")
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("status", "UP");
        response.put("service", "railway-ticket-risk-system");
        response.put("time", LocalDateTime.now());
        return response;
    }
}
