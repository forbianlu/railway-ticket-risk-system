package com.example.railway.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Train;
import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.TrainRepository;

@Service
public class TrainQueryService {

    private final TrainRepository trainRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    public TrainQueryService(TrainRepository trainRepository, SeatInventoryRepository seatInventoryRepository) {
        this.trainRepository = trainRepository;
        this.seatInventoryRepository = seatInventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainSearchResponse> search(String departureCode, String arrivalCode, LocalDate travelDate) {
        List<Train> trains = trainRepository.findByDepartureStation_CodeAndArrivalStation_CodeAndEnabledTrue(departureCode, arrivalCode);
        List<Long> trainIds = new ArrayList<Long>();
        for (Train train : trains) {
            trainIds.add(train.getId());
        }
        if (trainIds.isEmpty()) {
            return new ArrayList<TrainSearchResponse>();
        }

        List<SeatInventory> inventories = seatInventoryRepository.findByTrain_IdInAndTravelDate(trainIds, travelDate);
        List<TrainSearchResponse> responses = new ArrayList<TrainSearchResponse>();
        for (SeatInventory inventory : inventories) {
            responses.add(TrainSearchResponse.from(inventory));
        }
        return responses;
    }
}
