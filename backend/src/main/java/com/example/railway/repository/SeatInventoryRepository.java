package com.example.railway.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.SeatInventory;

public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    List<SeatInventory> findByTrain_IdInAndTravelDate(Collection<Long> trainIds, LocalDate travelDate);
}
