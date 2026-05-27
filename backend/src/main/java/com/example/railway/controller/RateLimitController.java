package com.example.railway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.RateLimitSummary;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.RateLimitService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "限流管理", description = "接口限流模式、规则和拦截统计")
@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "查询限流配置和统计")
    @RequiredRole(UserRole.ADMIN)
    @GetMapping("/summary")
    public RateLimitSummary summary() {
        return rateLimitService.summary();
    }
}
