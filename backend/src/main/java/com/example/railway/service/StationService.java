package com.example.railway.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.Station;
import com.example.railway.repository.StationRepository;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<Station> listEnabledStations() {
        return stationRepository.findByEnabledTrueOrderByCityAscNameAsc();
    }
}
