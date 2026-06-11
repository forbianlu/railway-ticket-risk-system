package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.railway.domain.PassengerIdType;
import com.example.railway.domain.PassengerTraveler;

public interface PassengerTravelerRepository extends JpaRepository<PassengerTraveler, Long> {

    List<PassengerTraveler> findByUserIdOrderByDefaultTravelerDescUpdatedAtDesc(Long userId);

    Optional<PassengerTraveler> findByIdAndUserId(Long id, Long userId);

    Optional<PassengerTraveler> findByUserIdAndDefaultTravelerTrue(Long userId);

    boolean existsByUserIdAndPassengerNameAndIdTypeAndIdNo(Long userId, String passengerName, PassengerIdType idType, String idNo);

    boolean existsByUserIdAndPassengerNameAndIdTypeAndIdNoAndIdNot(Long userId, String passengerName, PassengerIdType idType, String idNo, Long id);

    @Query("select p from PassengerTraveler p " +
            "where lower(p.passengerName) like lower(concat('%', :keyword, '%')) " +
            "or lower(p.idNo) like lower(concat('%', :keyword, '%')) " +
            "or lower(p.phone) like lower(concat('%', :keyword, '%')) " +
            "order by p.updatedAt desc")
    List<PassengerTraveler> searchAdmin(@Param("keyword") String keyword, Pageable pageable);
}
