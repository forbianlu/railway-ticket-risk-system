package com.example.railway.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.OrderStatus;
import com.example.railway.dto.DashboardSummary;
import com.example.railway.dto.TrainOrderStat;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.repository.TicketOrderRepository;

@Service
public class DashboardService {

    private final TicketOrderRepository ticketOrderRepository;
    private final RiskEventRepository riskEventRepository;

    public DashboardService(TicketOrderRepository ticketOrderRepository, RiskEventRepository riskEventRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.riskEventRepository = riskEventRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        DashboardSummary summary = new DashboardSummary();
        summary.setTotalOrders(ticketOrderRepository.count());
        summary.setPaidOrders(ticketOrderRepository.countByStatus(OrderStatus.PAID));
        summary.setRefundedOrders(ticketOrderRepository.countByStatus(OrderStatus.REFUNDED));
        summary.setTotalRiskEvents(riskEventRepository.count());
        summary.setOpenRiskEvents(riskEventRepository.countByHandledFalse());
        summary.setPopularTrains(popularTrains());
        return summary;
    }

    private List<TrainOrderStat> popularTrains() {
        List<Object[]> rows = ticketOrderRepository.findPopularTrainStats(PageRequest.of(0, 5));
        List<TrainOrderStat> stats = new ArrayList<TrainOrderStat>();
        for (Object[] row : rows) {
            String trainNo = String.valueOf(row[0]);
            long count = ((Number) row[1]).longValue();
            stats.add(new TrainOrderStat(trainNo, count));
        }
        return stats;
    }
}
