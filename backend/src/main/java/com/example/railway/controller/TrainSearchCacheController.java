package com.example.railway.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.TrainSearchCacheService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "缓存管理", description = "车次查询缓存统计和清理")
@RestController
@RequestMapping("/api/cache/train-search")
public class TrainSearchCacheController {

    private final TrainSearchCacheService trainSearchCacheService;

    public TrainSearchCacheController(TrainSearchCacheService trainSearchCacheService) {
        this.trainSearchCacheService = trainSearchCacheService;
    }

    @Operation(summary = "查询车次查询缓存统计")
    @RequiredRole({UserRole.ADMIN, UserRole.RISK_OFFICER})
    @GetMapping
    public TrainSearchCacheStats stats() {
        return trainSearchCacheService.stats();
    }

    @Operation(summary = "清空车次查询缓存")
    @RequiredRole(UserRole.ADMIN)
    @DeleteMapping
    public TrainSearchCacheStats clear() {
        return trainSearchCacheService.clear();
    }
}
