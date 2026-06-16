package com.example.railway.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.OperationLog;
import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.NotificationRecord;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.TicketRecord;
import com.example.railway.dto.OperationLogItemResponse;
import com.example.railway.dto.OrderDetailResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.OutboxEventResponse;
import com.example.railway.dto.NotificationResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.RefundResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.TicketResponse;
import com.example.railway.repository.OperationLogRepository;
import com.example.railway.repository.OutboxEventRepository;
import com.example.railway.repository.NotificationRecordRepository;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.security.AuthorizationException;

@Service
public class OrderDetailService {

    private final TicketOrderRepository ticketOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final RiskEventRepository riskEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OperationLogRepository operationLogRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final TicketService ticketService;
    private final TicketChangeService ticketChangeService;

    public OrderDetailService(TicketOrderRepository ticketOrderRepository,
                              PaymentRecordRepository paymentRecordRepository,
                              RefundRecordRepository refundRecordRepository,
                              RiskEventRepository riskEventRepository,
                              OutboxEventRepository outboxEventRepository,
                              OperationLogRepository operationLogRepository,
                              NotificationRecordRepository notificationRecordRepository,
                              TicketService ticketService,
                              TicketChangeService ticketChangeService) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.riskEventRepository = riskEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.operationLogRepository = operationLogRepository;
        this.notificationRecordRepository = notificationRecordRepository;
        this.ticketService = ticketService;
        this.ticketChangeService = ticketChangeService;
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse passengerDetail(Long orderId, Long userId) {
        TicketOrder order = ticketOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AuthorizationException("Order does not exist or is not owned by current passenger"));
        OrderDetailResponse response = basicDetail(order);
        response.setRisks(new ArrayList<RiskEventResponse>());
        response.setOutboxEvents(new ArrayList<OutboxEventResponse>());
        response.setOperationLogs(new ArrayList<OperationLogItemResponse>());
        return response;
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse adminDetail(Long orderId) {
        TicketOrder order = ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order does not exist"));
        OrderDetailResponse response = basicDetail(order);
        response.setRisks(toRiskResponses(riskEventRepository.findByOrder_IdOrderByCreatedAtDesc(orderId)));
        response.setOutboxEvents(toOutboxResponses(findOutboxChain(orderId)));
        response.setOperationLogs(toLogResponses(operationLogRepository.findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                "ORDER",
                String.valueOf(orderId)
        )));
        return response;
    }

    private List<OutboxEvent> findOutboxChain(Long orderId) {
        List<OutboxEvent> events = new ArrayList<OutboxEvent>();
        events.addAll(outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc("ORDER", String.valueOf(orderId)));
        for (PaymentRecord payment : paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(orderId)) {
            events.addAll(outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                    "PAYMENT",
                    String.valueOf(payment.getId())
            ));
        }
        for (RefundRecord refund : refundRecordRepository.findByOrderIdOrderByCreatedAtDesc(orderId)) {
            events.addAll(outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                    "REFUND",
                    String.valueOf(refund.getId())
            ));
        }
        TicketRecord ticket = ticketService.findByOrderId(orderId);
        if (ticket != null) {
            events.addAll(outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(
                    "TICKET",
                    String.valueOf(ticket.getId())
            ));
        }
        return events;
    }

    private OrderDetailResponse basicDetail(TicketOrder order) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrder(OrderResponse.from(order));
        response.setTicket(TicketResponse.from(ticketService.findByOrderId(order.getId())));
        response.setPayments(toPaymentResponses(paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())));
        response.setRefunds(toRefundResponses(refundRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())));
        response.setTicketChanges(ticketChangeService.findOrderChanges(order.getId()));
        response.setNotifications(toNotificationResponses(notificationRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())));
        return response;
    }

    private List<NotificationResponse> toNotificationResponses(List<NotificationRecord> records) {
        List<NotificationResponse> responses = new ArrayList<NotificationResponse>();
        for (NotificationRecord record : records) {
            responses.add(NotificationResponse.from(record));
        }
        return responses;
    }

    private List<PaymentResponse> toPaymentResponses(List<PaymentRecord> records) {
        List<PaymentResponse> responses = new ArrayList<PaymentResponse>();
        for (PaymentRecord record : records) {
            responses.add(PaymentResponse.from(record));
        }
        return responses;
    }

    private List<RefundResponse> toRefundResponses(List<RefundRecord> records) {
        List<RefundResponse> responses = new ArrayList<RefundResponse>();
        for (RefundRecord record : records) {
            responses.add(RefundResponse.from(record));
        }
        return responses;
    }

    private List<RiskEventResponse> toRiskResponses(List<RiskEvent> records) {
        List<RiskEventResponse> responses = new ArrayList<RiskEventResponse>();
        for (RiskEvent record : records) {
            responses.add(RiskEventResponse.from(record));
        }
        return responses;
    }

    private List<OutboxEventResponse> toOutboxResponses(List<OutboxEvent> records) {
        List<OutboxEventResponse> responses = new ArrayList<OutboxEventResponse>();
        for (OutboxEvent record : records) {
            responses.add(OutboxEventResponse.from(record));
        }
        return responses;
    }

    private List<OperationLogItemResponse> toLogResponses(List<OperationLog> records) {
        List<OperationLogItemResponse> responses = new ArrayList<OperationLogItemResponse>();
        for (OperationLog record : records) {
            responses.add(OperationLogItemResponse.from(record));
        }
        return responses;
    }
}
