package com.example.railway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.Train;

public interface TrainRepository extends JpaRepository<Train, Long> {

    List<Train> findByDepartureStation_CodeAndArrivalStation_CodeAndEnabledTrue(String departureCode, String arrivalCode);
}
