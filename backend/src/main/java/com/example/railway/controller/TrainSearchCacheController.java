package com.example.railway.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.TrainSearchCacheService;

@RestController
@RequestMapping("/api/cache/train-search")
public class TrainSearchCacheController {

    private final TrainSearchCacheService trainSearchCacheService;

    public TrainSearchCacheController(TrainSearchCacheService trainSearchCacheService) {
        this.trainSearchCacheService = trainSearchCacheService;
    }

    @RequiredRole({UserRole.ADMIN, UserRole.RISK_OFFICER})
    @GetMapping
    public TrainSearchCacheStats stats() {
        return trainSearchCacheService.stats();
    }

    @RequiredRole(UserRole.ADMIN)
    @DeleteMapping
    public TrainSearchCacheStats clear() {
        return trainSearchCacheService.clear();
    }
}
