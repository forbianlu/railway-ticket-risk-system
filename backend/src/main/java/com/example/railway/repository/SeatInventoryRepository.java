package com.example.railway.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.SeatInventory;

public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    List<SeatInventory> findByTrain_IdInAndTravelDate(Collection<Long> trainIds, LocalDate travelDate);

    Optional<SeatInventory> findByTrain_TrainNoAndTravelDateAndSeatType(String trainNo, LocalDate travelDate, String seatType);

    @EntityGraph(attributePaths = {"train", "train.departureStation", "train.arrivalStation"})
    @Query("select inventory from SeatInventory inventory " +
            "join inventory.train train " +
            "join train.departureStation departureStation " +
            "join train.arrivalStation arrivalStation " +
            "where train.enabled = true " +
            "and inventory.remainingSeats > 0 " +
            "and inventory.travelDate >= :today " +
            "and (:travelDate is null or inventory.travelDate = :travelDate) " +
            "and (:departureCode is null or departureStation.code = :departureCode or departureStation.name = :departureCode) " +
            "and (:arrivalCode is null or arrivalStation.code = :arrivalCode or arrivalStation.name = :arrivalCode) " +
            "order by inventory.travelDate asc, train.departureTime asc, train.trainNo asc, inventory.seatType asc")
    List<SeatInventory> findAvailableForSale(@Param("departureCode") String departureCode,
                                             @Param("arrivalCode") String arrivalCode,
                                             @Param("travelDate") LocalDate travelDate,
                                             @Param("today") LocalDate today,
                                             Pageable pageable);
}
