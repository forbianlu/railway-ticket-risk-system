package com.example.railway.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.railway.domain.Station;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByEnabledTrueOrderByCityAscNameAsc();

    Optional<Station> findByCode(String code);
}
