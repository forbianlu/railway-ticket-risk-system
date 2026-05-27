package com.example.railway.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.SeatInventory;

public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    List<SeatInventory> findByTrain_IdInAndTravelDate(Collection<Long> trainIds, LocalDate travelDate);

    Optional<SeatInventory> findByTrain_TrainNoAndTravelDateAndSeatType(String trainNo, LocalDate travelDate, String seatType);
}
