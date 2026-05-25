package com.example.railway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.RateLimitSummary;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.RateLimitService;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @RequiredRole(UserRole.ADMIN)
    @GetMapping("/summary")
    public RateLimitSummary summary() {
        return rateLimitService.summary();
    }
}
