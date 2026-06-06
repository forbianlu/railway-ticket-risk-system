package com.example.railway.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.MaskingUtils;
import com.example.railway.common.BusinessException;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.TicketRecord;
import com.example.railway.domain.TicketStatus;
import com.example.railway.repository.TicketRecordRepository;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@Service
public class TicketService {

    private static final DateTimeFormatter TICKET_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TicketRecordRepository ticketRecordRepository;
    private final OperationLogService operationLogService;
    private final OutboxEventPublisher outboxEventPublisher;

    public TicketService(TicketRecordRepository ticketRecordRepository,
                         OperationLogService operationLogService,
                         OutboxEventPublisher outboxEventPublisher) {
        this.ticketRecordRepository = ticketRecordRepository;
        this.operationLogService = operationLogService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public TicketRecord issueTicketForPaidOrder(TicketOrder order) {
        TicketRecord existing = ticketRecordRepository.findByOrderId(order.getId()).orElse(null);
        if (existing != null) {
            return existing;
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new BusinessException("Only PAID orders can issue tickets");
        }
        TicketRecord record = buildTicket(order, TicketStatus.ISSUED);
        record.setIssuedAt(order.getPaidAt() == null ? LocalDateTime.now() : order.getPaidAt());
        TicketRecord saved = ticketRecordRepository.save(record);
        operationLogService.record(
                "SYSTEM",
                "ISSUE_TICKET",
                "ORDER",
                String.valueOf(order.getId()),
                "Ticket " + saved.getTicketNo() + " issued for order " + order.getOrderNo()
        );
        publishTicketEvent(OutboxEventTypes.TICKET_ISSUED, saved);
        return saved;
    }

    @Transactional
    public TicketRecord refundTicketForOrder(TicketOrder order) {
        TicketRecord record = ticketRecordRepository.findByOrderId(order.getId()).orElse(null);
        if (record == null) {
            record = buildTicket(order, TicketStatus.REFUNDED);
            record.setIssuedAt(order.getPaidAt() == null ? LocalDateTime.now() : order.getPaidAt());
        }
        if (TicketStatus.REFUNDED.equals(record.getStatus())) {
            return record;
        }
        LocalDateTime now = order.getRefundedAt() == null ? LocalDateTime.now() : order.getRefundedAt();
        record.setStatus(TicketStatus.REFUNDED);
        record.setInvalidatedAt(now);
        record.setUpdatedAt(now);
        TicketRecord saved = ticketRecordRepository.save(record);
        operationLogService.record(
                "SYSTEM",
                "REFUND_TICKET",
                "ORDER",
                String.valueOf(order.getId()),
                "Ticket " + saved.getTicketNo() + " refunded for order " + order.getOrderNo()
        );
        publishTicketEvent(OutboxEventTypes.TICKET_REFUNDED, saved);
        return saved;
    }

    @Transactional
    public TicketRecord cancelTicketForOrder(TicketOrder order) {
        TicketRecord record = ticketRecordRepository.findByOrderId(order.getId()).orElse(null);
        if (record == null || TicketStatus.CANCELLED.equals(record.getStatus())) {
            return record;
        }
        LocalDateTime now = order.getClosedAt() == null ? LocalDateTime.now() : order.getClosedAt();
        record.setStatus(TicketStatus.CANCELLED);
        record.setInvalidatedAt(now);
        record.setUpdatedAt(now);
        TicketRecord saved = ticketRecordRepository.save(record);
        operationLogService.record(
                "SYSTEM",
                "CANCEL_TICKET",
                "ORDER",
                String.valueOf(order.getId()),
                "Ticket " + saved.getTicketNo() + " cancelled for order " + order.getOrderNo()
        );
        publishTicketEvent(OutboxEventTypes.TICKET_CANCELLED, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public TicketRecord findByOrderId(Long orderId) {
        return ticketRecordRepository.findByOrderId(orderId).orElse(null);
    }

    private TicketRecord buildTicket(TicketOrder order, TicketStatus status) {
        LocalDateTime now = LocalDateTime.now();
        TicketRecord record = new TicketRecord();
        record.setTicketNo(generateTicketNo(order));
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUserId(order.getUserId());
        record.setTrainNo(order.getTrain().getTrainNo());
        record.setDepartureStation(order.getTrain().getDepartureStation().getName());
        record.setArrivalStation(order.getTrain().getArrivalStation().getName());
        record.setTravelDate(order.getTravelDate());
        record.setDepartureTime(order.getTrain().getDepartureTime());
        record.setArrivalTime(order.getTrain().getArrivalTime());
        record.setSeatType(order.getSeatType());
        record.setPassengerName(order.getPassengerName());
        record.setPassengerIdCardMasked(order.getPassengerIdNoMasked() == null
                ? MaskingUtils.maskIdNo(order.getPassengerIdCard())
                : order.getPassengerIdNoMasked());
        record.setPassengerIdType(order.getPassengerIdType());
        record.setPassengerPhoneMasked(order.getPassengerPhoneMasked());
        record.setAmount(order.getAmount());
        record.setStatus(status);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private String generateTicketNo(TicketOrder order) {
        String date = order.getTravelDate() == null
                ? LocalDateTime.now().format(TICKET_DATE_FORMATTER)
                : order.getTravelDate().format(TICKET_DATE_FORMATTER);
        return String.format("ET%s%08d", date, order.getId());
    }

    private void publishTicketEvent(String eventType, TicketRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ticketId", record.getId());
        payload.put("ticketNo", record.getTicketNo());
        payload.put("orderId", record.getOrderId());
        payload.put("orderNo", record.getOrderNo());
        payload.put("userId", record.getUserId());
        payload.put("status", record.getStatus() == null ? null : record.getStatus().name());
        outboxEventPublisher.publish(eventType, "TICKET", String.valueOf(record.getId()), payload);
    }
}
