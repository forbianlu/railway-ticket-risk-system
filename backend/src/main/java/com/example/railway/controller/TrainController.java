package com.example.railway.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.service.TrainQueryService;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainQueryService trainQueryService;

    public TrainController(TrainQueryService trainQueryService) {
        this.trainQueryService = trainQueryService;
    }

    @GetMapping("/search")
    public List<TrainSearchResponse> search(@RequestParam("from") String departureCode,
                                            @RequestParam("to") String arrivalCode,
                                            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate) {
        return trainQueryService.search(departureCode, arrivalCode, travelDate);
    }
}
