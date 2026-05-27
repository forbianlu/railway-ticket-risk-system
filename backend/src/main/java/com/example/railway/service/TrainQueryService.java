package com.example.railway.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Train;
import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.TrainRepository;

@Service
public class TrainQueryService {

    private final TrainRepository trainRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final TrainSearchCacheService trainSearchCacheService;

    public TrainQueryService(TrainRepository trainRepository,
                             SeatInventoryRepository seatInventoryRepository,
                             TrainSearchCacheService trainSearchCacheService) {
        this.trainRepository = trainRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.trainSearchCacheService = trainSearchCacheService;
    }

    @Transactional(readOnly = true)
    public List<TrainSearchResponse> search(String departureCode, String arrivalCode, LocalDate travelDate) {
        List<TrainSearchResponse> cached = trainSearchCacheService.get(departureCode, arrivalCode, travelDate);
        if (cached != null) {
            return cached;
        }

        List<Train> trains = trainRepository.findByDepartureStation_CodeAndArrivalStation_CodeAndEnabledTrue(departureCode, arrivalCode);
        List<Long> trainIds = new ArrayList<Long>();
        for (Train train : trains) {
            trainIds.add(train.getId());
        }
        if (trainIds.isEmpty()) {
            List<TrainSearchResponse> responses = new ArrayList<TrainSearchResponse>();
            trainSearchCacheService.put(departureCode, arrivalCode, travelDate, responses);
            return responses;
        }

        List<SeatInventory> inventories = seatInventoryRepository.findByTrain_IdInAndTravelDate(trainIds, travelDate);
        List<TrainSearchResponse> responses = new ArrayList<TrainSearchResponse>();
        for (SeatInventory inventory : inventories) {
            responses.add(TrainSearchResponse.from(inventory));
        }
        trainSearchCacheService.put(departureCode, arrivalCode, travelDate, responses);
        return responses;
    }

    @Transactional(readOnly = true)
    public List<TrainSearchResponse> available(String departureCode,
                                               String arrivalCode,
                                               LocalDate travelDate,
                                               int page,
                                               int size) {
        if (page < 0) {
            throw new BusinessException("page must be greater than or equal to 0");
        }
        if (size <= 0) {
            throw new BusinessException("size must be greater than 0");
        }
        int normalizedSize = Math.min(size, 100);
        List<SeatInventory> inventories = seatInventoryRepository.findAvailableForSale(
                normalize(departureCode),
                normalize(arrivalCode),
                travelDate,
                LocalDate.now(),
                PageRequest.of(page, normalizedSize)
        );
        List<TrainSearchResponse> responses = new ArrayList<TrainSearchResponse>();
        for (SeatInventory inventory : inventories) {
            responses.add(TrainSearchResponse.from(inventory));
        }
        return responses;
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
