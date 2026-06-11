package com.example.railway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.domain.UserRole;
import com.example.railway.dto.AdminGlobalSearchResponse;
import com.example.railway.security.RequiredRole;
import com.example.railway.service.AdminSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Global Search", description = "Management-side cross-domain search and trace lookup")
@RequiredRole({UserRole.ADMIN, UserRole.OPERATOR, UserRole.RISK_OFFICER})
@RestController
@RequestMapping("/api/search")
public class AdminSearchController {

    private final AdminSearchService adminSearchService;

    public AdminSearchController(AdminSearchService adminSearchService) {
        this.adminSearchService = adminSearchService;
    }

    @Operation(summary = "Search orders, tickets, payments, refunds, notifications, risk events, outbox events and logs")
    @GetMapping
    public AdminGlobalSearchResponse search(@RequestParam("keyword") String keyword,
                                            @RequestParam(value = "types", required = false) String types,
                                            @RequestParam(value = "limitPerType", required = false) Integer limitPerType,
                                            @RequestParam(value = "includeTrace", required = false) Boolean includeTrace) {
        return adminSearchService.search(keyword, types, limitPerType, includeTrace);
    }
}
