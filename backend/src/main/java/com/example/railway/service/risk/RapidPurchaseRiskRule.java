package com.example.railway.service.risk;

import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.railway.domain.RiskLevel;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskType;
import com.example.railway.domain.TicketOrder;
import com.example.railway.repository.TicketOrderRepository;

@Order(10)
@Component
public class RapidPurchaseRiskRule implements RiskRule {

    private static final long RAPID_PURCHASE_LIMIT = 3L;

    private final TicketOrderRepository ticketOrderRepository;

    public RapidPurchaseRiskRule(TicketOrderRepository ticketOrderRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
    }

    @Override
    public RiskScene scene() {
        return RiskScene.ORDER_CREATED;
    }

    @Override
    public Optional<RiskHit> evaluate(RiskContext context) {
        TicketOrder order = context.getOrder();
        long recentOrders = ticketOrderRepository.countByUserIdAndCreatedAtAfter(
                order.getUserId(),
                context.getEvaluatedAt().minusMinutes(10)
        );
        if (recentOrders < RAPID_PURCHASE_LIMIT) {
            return Optional.empty();
        }
        return Optional.of(new RiskHit(
                RiskType.RAPID_PURCHASE,
                RiskLevel.MEDIUM,
                "用户 10 分钟内下单次数达到 " + recentOrders + " 次"
        ));
    }
}
