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

    @GetMapping("/search")
    public List<TrainSearchResponse> search(@RequestParam("from") String departureCode,
                                            @RequestParam("to") String arrivalCode,
                                            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
                                            HttpServletRequest request) {
        rateLimitService.check("rate:train:search:" + requesterKey(request), 120, 60);
        return trainQueryService.search(departureCode, arrivalCode, travelDate);
    }

    private String requesterKey(HttpServletRequest request) {
        AuthPrincipal principal = AuthContext.currentOrNull();
        if (principal != null && principal.getUserId() != null) {
            return "user:" + principal.getUserId();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
