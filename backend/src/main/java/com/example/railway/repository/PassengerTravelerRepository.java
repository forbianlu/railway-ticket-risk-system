package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.PassengerIdType;
import com.example.railway.domain.PassengerTraveler;

public interface PassengerTravelerRepository extends JpaRepository<PassengerTraveler, Long> {

    List<PassengerTraveler> findByUserIdOrderByDefaultTravelerDescUpdatedAtDesc(Long userId);

    Optional<PassengerTraveler> findByIdAndUserId(Long id, Long userId);

    Optional<PassengerTraveler> findByUserIdAndDefaultTravelerTrue(Long userId);

    boolean existsByUserIdAndPassengerNameAndIdTypeAndIdNo(Long userId, String passengerName, PassengerIdType idType, String idNo);

    boolean existsByUserIdAndPassengerNameAndIdTypeAndIdNoAndIdNot(Long userId, String passengerName, PassengerIdType idType, String idNo, Long id);
}
