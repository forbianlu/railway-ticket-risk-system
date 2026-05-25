package com.example.railway.service.cache;

import java.time.LocalDate;
import java.util.List;

import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;

public interface TrainSearchCacheStore {

    String mode();

    List<TrainSearchResponse> get(String departureCode, String arrivalCode, LocalDate travelDate);

    void put(String departureCode, String arrivalCode, LocalDate travelDate, List<TrainSearchResponse> responses);

    void evictRoute(String departureCode, String arrivalCode, LocalDate travelDate);

    TrainSearchCacheStats clear();

    TrainSearchCacheStats stats();
}
