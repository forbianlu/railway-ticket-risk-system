package com.example.railway.controller;

import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.security.AuthContext;
import com.example.railway.security.AuthPrincipal;
import com.example.railway.service.RateLimitService;
import com.example.railway.service.TrainQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "车次查询", description = "车站、车次和余票查询")
@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainQueryService trainQueryService;
    private final RateLimitService rateLimitService;

    public TrainController(TrainQueryService trainQueryService,
                           RateLimitService rateLimitService) {
        this.trainQueryService = trainQueryService;
        this.rateLimitService = rateLimitService;
    }

    @Operation(summary = "按出发站、到达站和日期查询车次余票")
    @GetMapping("/search")
    public List<TrainSearchResponse> search(@RequestParam("from") String departureCode,
                                            @RequestParam("to") String arrivalCode,
                                            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
                                            HttpServletRequest request) {
        rateLimitService.check("train-search", "rate:train:search:" + requesterKey(request));
        return trainQueryService.search(departureCode, arrivalCode, travelDate);
    }

    @Operation(summary = "查询全部可购买车次")
    @GetMapping("/available")
    public List<TrainSearchResponse> available(@RequestParam(value = "fromStation", required = false) String fromStation,
                                               @RequestParam(value = "toStation", required = false) String toStation,
                                               @RequestParam(value = "from", required = false) String fromAlias,
                                               @RequestParam(value = "to", required = false) String toAlias,
                                               @RequestParam(value = "travelDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
                                               @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateAlias,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "50") int size,
                                               HttpServletRequest request) {
        rateLimitService.check("train-search", "rate:train:available:" + requesterKey(request));
        return trainQueryService.available(
                firstNonBlank(fromStation, fromAlias),
                firstNonBlank(toStation, toAlias),
                travelDate == null ? dateAlias : travelDate,
                page,
                size
        );
    }

    private String requesterKey(HttpServletRequest request) {
        AuthPrincipal principal = AuthContext.currentOrNull();
        if (principal != null && principal.getUserId() != null) {
            return "user:" + principal.getUserId();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        return fallback;
    }
}
