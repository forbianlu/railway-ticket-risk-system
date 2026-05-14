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
        long totalOrderCount = ticketOrderRepository.count();
        long pendingPaymentOrderCount = ticketOrderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long paidOrderCount = ticketOrderRepository.countByStatus(OrderStatus.PAID);
        long closedOrderCount = ticketOrderRepository.countByStatus(OrderStatus.CLOSED);
        long refundedOrderCount = ticketOrderRepository.countByStatus(OrderStatus.REFUNDED);
        long totalRiskEventCount = riskEventRepository.count();
        long unhandledRiskCount = riskEventRepository.countByHandledFalse();
        long effectiveOrderCount = paidOrderCount + refundedOrderCount;

        DashboardSummary summary = new DashboardSummary();
        summary.setTotalOrders(totalOrderCount);
        summary.setPaidOrders(paidOrderCount);
        summary.setRefundedOrders(refundedOrderCount);
        summary.setTotalRiskEvents(totalRiskEventCount);
        summary.setOpenRiskEvents(unhandledRiskCount);
        summary.setTotalOrderCount(totalOrderCount);
        summary.setPendingPaymentOrderCount(pendingPaymentOrderCount);
        summary.setPaidOrderCount(paidOrderCount);
        summary.setClosedOrderCount(closedOrderCount);
        summary.setRefundedOrderCount(refundedOrderCount);
        summary.setUnhandledRiskCount(unhandledRiskCount);
        summary.setRefundRate(rate(refundedOrderCount, effectiveOrderCount));
        summary.setRiskRate(rate(totalRiskEventCount, effectiveOrderCount));
        summary.setPopularTrains(popularTrains());
        return summary;
    }

    private double rate(long numerator, long denominator) {
        return numerator * 1.0D / Math.max(1L, denominator);
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
