package com.example.railway.dto;

import java.util.ArrayList;
import java.util.List;

public class OrderDetailResponse {

    private OrderResponse order;
    private TicketResponse ticket;
    private List<PaymentResponse> payments = new ArrayList<PaymentResponse>();
    private List<RefundResponse> refunds = new ArrayList<RefundResponse>();
    private List<TicketChangeResponse> ticketChanges = new ArrayList<TicketChangeResponse>();
    private List<NotificationResponse> notifications = new ArrayList<NotificationResponse>();
    private List<RiskEventResponse> risks = new ArrayList<RiskEventResponse>();
    private List<OutboxEventResponse> outboxEvents = new ArrayList<OutboxEventResponse>();
    private List<OperationLogItemResponse> operationLogs = new ArrayList<OperationLogItemResponse>();

    public OrderResponse getOrder() {
        return order;
    }

    public void setOrder(OrderResponse order) {
        this.order = order;
    }

    public TicketResponse getTicket() {
        return ticket;
    }

    public void setTicket(TicketResponse ticket) {
        this.ticket = ticket;
    }

    public List<PaymentResponse> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentResponse> payments) {
        this.payments = payments;
    }

    public List<RefundResponse> getRefunds() {
        return refunds;
    }

    public void setRefunds(List<RefundResponse> refunds) {
        this.refunds = refunds;
    }

    public List<TicketChangeResponse> getTicketChanges() {
        return ticketChanges;
    }

    public void setTicketChanges(List<TicketChangeResponse> ticketChanges) {
        this.ticketChanges = ticketChanges;
    }

    public List<NotificationResponse> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationResponse> notifications) {
        this.notifications = notifications;
    }

    public List<RiskEventResponse> getRisks() {
        return risks;
    }

    public void setRisks(List<RiskEventResponse> risks) {
        this.risks = risks;
    }

    public List<OutboxEventResponse> getOutboxEvents() {
        return outboxEvents;
    }

    public void setOutboxEvents(List<OutboxEventResponse> outboxEvents) {
        this.outboxEvents = outboxEvents;
    }

    public List<OperationLogItemResponse> getOperationLogs() {
        return operationLogs;
    }

    public void setOperationLogs(List<OperationLogItemResponse> operationLogs) {
        this.operationLogs = operationLogs;
    }
}
