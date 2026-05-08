package com.example.railway.service.risk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.railway.domain.RiskLevel;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskType;
import com.example.railway.domain.TicketOrder;
import com.example.railway.repository.TicketOrderRepository;

@Order(20)
@Component
public class HighAmountRiskRule implements RiskRule {

    private static final BigDecimal DAILY_AMOUNT_LIMIT = new BigDecimal("1000.00");

    private final TicketOrderRepository ticketOrderRepository;

    public HighAmountRiskRule(TicketOrderRepository ticketOrderRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
    }

    @Override
    public RiskScene scene() {
        return RiskScene.ORDER_CREATED;
    }

    @Override
    public Optional<RiskHit> evaluate(RiskContext context) {
        TicketOrder order = context.getOrder();
        BigDecimal todayAmount = ticketOrderRepository.sumPaidAmountByUserAfter(
                order.getUserId(),
                context.getEvaluatedAt().toLocalDate().atStartOfDay()
        );
        if (todayAmount.compareTo(DAILY_AMOUNT_LIMIT) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new RiskHit(
                RiskType.HIGH_AMOUNT,
                RiskLevel.HIGH,
                "用户当日有效订单金额达到 " + todayAmount + " 元"
        ));
    }
}
