package com.example.railway.service.risk;

import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.RiskLevel;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskType;
import com.example.railway.domain.TicketOrder;
import com.example.railway.repository.TicketOrderRepository;

@Order(30)
@Component
public class FrequentRefundRiskRule implements RiskRule {

    private static final long FREQUENT_REFUND_LIMIT = 3L;

    private final TicketOrderRepository ticketOrderRepository;

    public FrequentRefundRiskRule(TicketOrderRepository ticketOrderRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
    }

    @Override
    public RiskScene scene() {
        return RiskScene.ORDER_REFUNDED;
    }

    @Override
    public Optional<RiskHit> evaluate(RiskContext context) {
        TicketOrder order = context.getOrder();
        long refundCount = ticketOrderRepository.countByUserIdAndStatusAndRefundedAtAfter(
                order.getUserId(),
                OrderStatus.REFUNDED,
                context.getEvaluatedAt().minusDays(7)
        );
        if (refundCount < FREQUENT_REFUND_LIMIT) {
            return Optional.empty();
        }
        return Optional.of(new RiskHit(
                RiskType.FREQUENT_REFUND,
                RiskLevel.MEDIUM,
                "用户 7 天内退票次数达到 " + refundCount + " 次"
        ));
    }
}
