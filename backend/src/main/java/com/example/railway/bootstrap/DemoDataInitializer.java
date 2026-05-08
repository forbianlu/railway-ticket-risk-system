package com.example.railway.bootstrap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Station;
import com.example.railway.domain.Train;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.StationRepository;
import com.example.railway.repository.TrainRepository;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final SeatInventoryRepository seatInventoryRepository;

    public DemoDataInitializer(StationRepository stationRepository,
                               TrainRepository trainRepository,
                               SeatInventoryRepository seatInventoryRepository) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.seatInventoryRepository = seatInventoryRepository;
    }

    @Override
    public void run(String... args) {
        if (stationRepository.count() > 0) {
            return;
        }

        Station beijing = stationRepository.save(new Station("BJP", "北京南", "北京"));
        Station shanghai = stationRepository.save(new Station("SHH", "上海虹桥", "上海"));
        Station wuhan = stationRepository.save(new Station("WHN", "武汉", "武汉"));
        Station guangzhou = stationRepository.save(new Station("GZQ", "广州南", "广州"));

        Train g101 = trainRepository.save(new Train("G101", beijing, shanghai, LocalTime.of(7, 0), LocalTime.of(12, 38)));
        Train g305 = trainRepository.save(new Train("G305", beijing, guangzhou, LocalTime.of(9, 15), LocalTime.of(18, 20)));
        Train d701 = trainRepository.save(new Train("D701", wuhan, shanghai, LocalTime.of(8, 30), LocalTime.of(14, 5)));

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate travelDate = today.plusDays(i);
            seatInventoryRepository.save(new SeatInventory(g101, travelDate, "SECOND_CLASS", 180, new BigDecimal("553.00")));
            seatInventoryRepository.save(new SeatInventory(g101, travelDate, "FIRST_CLASS", 40, new BigDecimal("933.00")));
            seatInventoryRepository.save(new SeatInventory(g305, travelDate, "SECOND_CLASS", 220, new BigDecimal("862.00")));
            seatInventoryRepository.save(new SeatInventory(g305, travelDate, "BUSINESS_CLASS", 20, new BigDecimal("2724.00")));
            seatInventoryRepository.save(new SeatInventory(d701, travelDate, "SECOND_CLASS", 160, new BigDecimal("315.00")));
        }
    }
}
