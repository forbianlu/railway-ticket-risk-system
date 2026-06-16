package com.example.railway.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.NotificationStatus;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.OutboxEventStatus;
import com.example.railway.domain.PaymentStatus;
import com.example.railway.domain.RefundStatus;
import com.example.railway.domain.TicketChangeStatus;
import com.example.railway.dto.AdminWorkbenchItemResponse;
import com.example.railway.dto.AdminWorkbenchResponse;
import com.example.railway.domain.RiskStatus;
import com.example.railway.dto.DashboardSummary;
import com.example.railway.dto.TrainOrderStat;
import com.example.railway.repository.NotificationRecordRepository;
import com.example.railway.repository.OutboxEventRepository;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.repository.TicketChangeRecordRepository;
import com.example.railway.repository.TicketOrderRepository;

@Service
public class DashboardService {

    private final TicketOrderRepository ticketOrderRepository;
    private final RiskEventRepository riskEventRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final TicketChangeRecordRepository ticketChangeRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final NotificationRecordRepository notificationRecordRepository;

    public DashboardService(TicketOrderRepository ticketOrderRepository,
                            RiskEventRepository riskEventRepository,
                            PaymentRecordRepository paymentRecordRepository,
                            RefundRecordRepository refundRecordRepository,
                            TicketChangeRecordRepository ticketChangeRecordRepository,
                            OutboxEventRepository outboxEventRepository,
                            NotificationRecordRepository notificationRecordRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.riskEventRepository = riskEventRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.ticketChangeRecordRepository = ticketChangeRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        long totalOrderCount = ticketOrderRepository.count();
        long pendingPaymentOrderCount = ticketOrderRepository.countByStatus(OrderStatus.PENDING_PAYMENT);
        long paidOrderCount = ticketOrderRepository.countByStatus(OrderStatus.PAID);
        long closedOrderCount = ticketOrderRepository.countByStatus(OrderStatus.CLOSED);
        long refundedOrderCount = ticketOrderRepository.countByStatus(OrderStatus.REFUNDED);
        long totalRiskEventCount = riskEventRepository.count();
        long unhandledRiskCount = riskEventRepository.countByStatus(RiskStatus.PENDING);
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

    @Transactional(readOnly = true)
    public AdminWorkbenchResponse workbench() {
        AdminWorkbenchResponse response = new AdminWorkbenchResponse();
        response.setFailedPaymentCount(paymentRecordRepository.countByStatus(PaymentStatus.FAILED));
        response.setPendingRefundCount(refundRecordRepository.countByStatus(RefundStatus.PENDING));
        response.setFailedRefundCount(refundRecordRepository.countByStatus(RefundStatus.FAILED));
        response.setPendingChangeCount(ticketChangeRecordRepository.countByStatus(TicketChangeStatus.PENDING_PAYMENT));
        response.setFailedChangeCount(ticketChangeRecordRepository.countByStatus(TicketChangeStatus.FAILED));
        response.setPendingRiskCount(riskEventRepository.countByStatus(RiskStatus.PENDING));
        response.setFailedOutboxCount(outboxEventRepository.countByStatus(OutboxEventStatus.FAILED));
        response.setBacklogOutboxCount(outboxEventRepository.countByStatus(OutboxEventStatus.PENDING)
                + outboxEventRepository.countByStatus(OutboxEventStatus.PROCESSING));
        response.setUnreadNotificationCount(notificationRecordRepository.countByStatus(NotificationStatus.UNREAD));
        response.setTotalExceptionCount(response.getFailedPaymentCount()
                + response.getPendingRefundCount()
                + response.getFailedRefundCount()
                + response.getPendingChangeCount()
                + response.getFailedChangeCount()
                + response.getPendingRiskCount()
                + response.getFailedOutboxCount()
                + response.getBacklogOutboxCount()
                + response.getUnreadNotificationCount());

        List<AdminWorkbenchItemResponse> items = new ArrayList<AdminWorkbenchItemResponse>();
        paymentRecordRepository.findTop8ByStatusOrderByCreatedAtDesc(PaymentStatus.FAILED)
                .forEach(payment -> items.add(item("PAYMENT_FAILED", "支付失败待排查",
                        payment.getOrderNo() + " / " + payment.getPaymentNo(),
                        payment.getStatus().name(), "high", "payments", "查看支付流水",
                        payment.getOrderId(), payment.getOrderNo(), payment.getPaymentNo(), payment.getCreatedAt())));
        refundRecordRepository.findTop8ByStatusOrderByCreatedAtDesc(RefundStatus.PENDING)
                .forEach(refund -> items.add(item("REFUND_PENDING", "退款处理中",
                        refund.getOrderNo() + " / " + refund.getRefundNo(),
                        refund.getStatus().name(), "medium", "refunds", "查看退款流水",
                        refund.getOrderId(), refund.getOrderNo(), refund.getRefundNo(), refund.getCreatedAt())));
        refundRecordRepository.findTop8ByStatusOrderByCreatedAtDesc(RefundStatus.FAILED)
                .forEach(refund -> items.add(item("REFUND_FAILED", "退款失败待处理",
                        refund.getOrderNo() + " / " + refund.getRefundNo(),
                        refund.getStatus().name(), "high", "refunds", "查看退款流水",
                        refund.getOrderId(), refund.getOrderNo(), refund.getRefundNo(), refund.getCreatedAt())));
        ticketChangeRecordRepository.findTop8ByStatusOrderByCreatedAtDesc(TicketChangeStatus.PENDING_PAYMENT)
                .forEach(change -> items.add(item("CHANGE_PENDING", "改签待支付差额",
                        change.getOriginalOrderNo() + " -> " + change.getNewOrderNo(),
                        change.getStatus().name(), "medium", "ticket-changes", "查看改签追踪",
                        change.getNewOrderId(), change.getNewOrderNo(), change.getChangeNo(), change.getCreatedAt())));
        ticketChangeRecordRepository.findTop8ByStatusOrderByCreatedAtDesc(TicketChangeStatus.FAILED)
                .forEach(change -> items.add(item("CHANGE_FAILED", "改签失败待排查",
                        change.getOriginalOrderNo() + " -> " + change.getNewOrderNo(),
                        change.getStatus().name(), "high", "ticket-changes", "查看改签追踪",
                        change.getNewOrderId(), change.getNewOrderNo(), change.getChangeNo(), change.getCreatedAt())));
        riskEventRepository.findTop50ByStatusOrderByCreatedAtDesc(RiskStatus.PENDING).stream()
                .limit(8)
                .forEach(risk -> items.add(item("RISK_PENDING", "待处置风险事件",
                        risk.getReason(),
                        risk.getStatus().name(), "high", "risks", "进入风险处置",
                        risk.getOrder() == null ? null : risk.getOrder().getId(),
                        risk.getOrder() == null ? null : risk.getOrder().getOrderNo(),
                        String.valueOf(risk.getId()), risk.getCreatedAt())));
        outboxEventRepository.findTop8ByStatusOrderByCreatedAtDesc(OutboxEventStatus.FAILED)
                .forEach(event -> items.add(item("OUTBOX_FAILED", "Outbox 事件失败",
                        event.getEventType() + " / " + event.getAggregateType() + ":" + event.getAggregateId(),
                        event.getStatus().name(), "medium", "outbox", "查看事件中心",
                        null, null, event.getEventId(), event.getCreatedAt())));

        items.sort(Comparator.comparing(AdminWorkbenchItemResponse::getCreatedAt).reversed());
        List<AdminWorkbenchItemResponse> limitedItems = items;
        if (items.size() > 12) {
            limitedItems = new ArrayList<AdminWorkbenchItemResponse>(items.subList(0, 12));
        }
        response.setExceptionItems(limitedItems);
        if (!limitedItems.isEmpty()) {
            response.setLatestCreatedAt(limitedItems.get(0).getCreatedAt());
        }
        return response;
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

    private AdminWorkbenchItemResponse item(String type,
                                            String title,
                                            String description,
                                            String status,
                                            String severity,
                                            String actionTarget,
                                            String actionLabel,
                                            Long orderId,
                                            String orderNo,
                                            String businessId,
                                            java.time.LocalDateTime createdAt) {
        AdminWorkbenchItemResponse item = new AdminWorkbenchItemResponse();
        item.setType(type);
        item.setTitle(title);
        item.setDescription(description);
        item.setStatus(status);
        item.setSeverity(severity);
        item.setActionTarget(actionTarget);
        item.setActionLabel(actionLabel);
        item.setOrderId(orderId);
        item.setOrderNo(orderNo);
        item.setBusinessId(businessId);
        item.setCreatedAt(createdAt);
        return item;
    }
}
