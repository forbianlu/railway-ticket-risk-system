package com.example.railway.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.RiskEventResponse;
import com.example.railway.service.RiskService;

@RestController
@RequestMapping("/api/risks")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping
    public List<RiskEventResponse> listRisks() {
        return riskService.latestRisks();
    }

    @PostMapping("/{id}/handle")
    public RiskEventResponse handleRisk(@PathVariable("id") Long riskId,
                                        @RequestParam(value = "operator", required = false) String operator) {
        return riskService.handleRisk(riskId, operator);
    }
}
