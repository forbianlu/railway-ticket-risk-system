package com.example.railway.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.NotificationRecord;
import com.example.railway.domain.OperationLog;
import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.PassengerTraveler;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.TicketRecord;
import com.example.railway.dto.AdminGlobalSearchResponse;
import com.example.railway.dto.SearchResultGroupResponse;
import com.example.railway.dto.SearchResultItemResponse;
import com.example.railway.dto.SearchType;
import com.example.railway.repository.NotificationRecordRepository;
import com.example.railway.repository.OperationLogRepository;
import com.example.railway.repository.OutboxEventRepository;
import com.example.railway.repository.PassengerTravelerRepository;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.repository.TicketRecordRepository;

@Service
public class AdminSearchService {

    private static final int DEFAULT_LIMIT_PER_TYPE = 5;
    private static final int MAX_LIMIT_PER_TYPE = 20;

    private final TicketOrderRepository orderRepository;
    private final TicketRecordRepository ticketRecordRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final PassengerTravelerRepository passengerTravelerRepository;
    private final RiskEventRepository riskEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final NotificationRecordRepository notificationRecordRepository;
    private final OperationLogRepository operationLogRepository;

    public AdminSearchService(TicketOrderRepository orderRepository,
                              TicketRecordRepository ticketRecordRepository,
                              PaymentRecordRepository paymentRecordRepository,
                              RefundRecordRepository refundRecordRepository,
                              PassengerTravelerRepository passengerTravelerRepository,
                              RiskEventRepository riskEventRepository,
                              OutboxEventRepository outboxEventRepository,
                              NotificationRecordRepository notificationRecordRepository,
                              OperationLogRepository operationLogRepository) {
        this.orderRepository = orderRepository;
        this.ticketRecordRepository = ticketRecordRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.passengerTravelerRepository = passengerTravelerRepository;
        this.riskEventRepository = riskEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.notificationRecordRepository = notificationRecordRepository;
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminGlobalSearchResponse search(String keyword, String types, Integer limitPerType, Boolean includeTrace) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Set<SearchType> selectedTypes = parseTypes(types);
        int limit = normalizeLimit(limitPerType);
        Pageable pageable = PageRequest.of(0, limit);
        boolean traceEnabled = Boolean.TRUE.equals(includeTrace);

        AdminGlobalSearchResponse response = new AdminGlobalSearchResponse();
        response.setKeyword(normalizedKeyword);

        List<SearchResultGroupResponse> groups = new ArrayList<SearchResultGroupResponse>();
        for (SearchType type : selectedTypes) {
            SearchResultGroupResponse group = searchGroup(type, normalizedKeyword, pageable, traceEnabled);
            groups.add(group);
            response.setTotalCount(response.getTotalCount() + group.getCount());
        }
        response.setGroups(groups);
        return response;
    }

    private SearchResultGroupResponse searchGroup(SearchType type, String keyword, Pageable pageable, boolean includeTrace) {
        if (SearchType.ORDER.equals(type)) {
            return group(type, mapOrders(searchOrders(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.TICKET.equals(type)) {
            return group(type, mapTickets(ticketRecordRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.PAYMENT.equals(type)) {
            return group(type, mapPayments(paymentRecordRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.REFUND.equals(type)) {
            return group(type, mapRefunds(refundRecordRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.TRAVELER.equals(type)) {
            return group(type, mapTravelers(passengerTravelerRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.RISK.equals(type)) {
            return group(type, mapRisks(searchRisks(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.OUTBOX.equals(type)) {
            return group(type, mapOutbox(outboxEventRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
        }
        if (SearchType.NOTIFICATION.equals(type)) {
            return group(type, mapNotifications(notificationRecordRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
        }
        return group(type, mapOperationLogs(operationLogRepository.searchAdmin(keyword, pageable), keyword, includeTrace));
    }

    private List<TicketOrder> searchOrders(String keyword, Pageable pageable) {
        if (isNumeric(keyword)) {
            Long numeric = Long.valueOf(keyword);
            List<TicketOrder> items = new ArrayList<TicketOrder>();
            orderRepository.findById(numeric).ifPresent(items::add);
            items.addAll(orderRepository.findByUserIdOrderByCreatedAtDesc(numeric, pageable));
            if (!items.isEmpty()) {
                return limitDistinctOrders(items, pageable.getPageSize());
            }
        }
        return orderRepository.searchAdmin(keyword, pageable);
    }

    private List<RiskEvent> searchRisks(String keyword, Pageable pageable) {
        if (isNumeric(keyword)) {
            return riskEventRepository.findByUserIdOrderByCreatedAtDesc(Long.valueOf(keyword), pageable);
        }
        return riskEventRepository.searchAdmin(keyword, pageable);
    }

    private List<TicketOrder> limitDistinctOrders(List<TicketOrder> source, int limit) {
        List<TicketOrder> result = new ArrayList<TicketOrder>();
        for (TicketOrder order : source) {
            boolean exists = false;
            for (TicketOrder current : result) {
                if (current.getId().equals(order.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                result.add(order);
            }
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<SearchResultItemResponse> mapOrders(List<TicketOrder> orders, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (TicketOrder order : orders) {
            SearchResultItemResponse item = base("ORDER-" + order.getId(),
                    order.getOrderNo(),
                    order.getPassengerName() + " | " + safeTrainNo(order) + " | " + safeRoute(order),
                    enumName(order.getStatus()),
                    "ORDER",
                    String.valueOf(order.getId()),
                    order.getCreatedAt());
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setDetailAction("ORDER_DETAIL");
            item.setMatchedFields(matched(keyword, "orderNo", order.getOrderNo(),
                    "passengerName", order.getPassengerName(),
                    "trainNo", safeTrainNo(order),
                    "route", safeRoute(order),
                    "userId", String.valueOf(order.getUserId())));
            if (includeTrace) {
                item.getTrace().add("ORDER -> PAYMENT/REFUND/TICKET/RISK/OUTBOX/LOG");
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapTickets(List<TicketRecord> tickets, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (TicketRecord ticket : tickets) {
            SearchResultItemResponse item = base("TICKET-" + ticket.getId(),
                    ticket.getTicketNo(),
                    ticket.getPassengerName() + " | " + ticket.getTrainNo() + " | " + ticket.getDepartureStation() + " -> " + ticket.getArrivalStation(),
                    enumName(ticket.getStatus()),
                    "TICKET",
                    ticket.getTicketNo(),
                    ticket.getCreatedAt());
            item.setOrderId(ticket.getOrderId());
            item.setOrderNo(ticket.getOrderNo());
            item.setTicketNo(ticket.getTicketNo());
            item.setDetailAction("ORDER_DETAIL");
            item.setMatchedFields(matched(keyword, "ticketNo", ticket.getTicketNo(),
                    "orderNo", ticket.getOrderNo(),
                    "passengerName", ticket.getPassengerName(),
                    "trainNo", ticket.getTrainNo()));
            if (includeTrace) {
                item.getTrace().add("TICKET -> ORDER_DETAIL");
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapPayments(List<PaymentRecord> payments, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (PaymentRecord payment : payments) {
            SearchResultItemResponse item = base("PAYMENT-" + payment.getId(),
                    payment.getPaymentNo(),
                    payment.getOrderNo() + " | " + payment.getChannel() + " | " + payment.getAmount(),
                    enumName(payment.getStatus()),
                    "PAYMENT",
                    payment.getPaymentNo(),
                    payment.getCreatedAt());
            item.setOrderId(payment.getOrderId());
            item.setOrderNo(payment.getOrderNo());
            item.setPaymentNo(payment.getPaymentNo());
            item.setDetailAction("ORDER_DETAIL");
            item.setMatchedFields(matched(keyword, "paymentNo", payment.getPaymentNo(),
                    "channelPaymentNo", payment.getChannelPaymentNo(),
                    "orderNo", payment.getOrderNo()));
            if (includeTrace) {
                item.getTrace().add("PAYMENT -> ORDER_DETAIL");
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapRefunds(List<RefundRecord> refunds, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (RefundRecord refund : refunds) {
            SearchResultItemResponse item = base("REFUND-" + refund.getId(),
                    refund.getRefundNo(),
                    refund.getOrderNo() + " | " + refund.getChannel() + " | " + refund.getAmount(),
                    enumName(refund.getStatus()),
                    "REFUND",
                    refund.getRefundNo(),
                    refund.getCreatedAt());
            item.setOrderId(refund.getOrderId());
            item.setOrderNo(refund.getOrderNo());
            item.setPaymentNo(refund.getPaymentNo());
            item.setRefundNo(refund.getRefundNo());
            item.setDetailAction("ORDER_DETAIL");
            item.setMatchedFields(matched(keyword, "refundNo", refund.getRefundNo(),
                    "channelRefundNo", refund.getChannelRefundNo(),
                    "paymentNo", refund.getPaymentNo(),
                    "orderNo", refund.getOrderNo()));
            if (includeTrace) {
                item.getTrace().add("REFUND -> ORDER_DETAIL");
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapTravelers(List<PassengerTraveler> travelers, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (PassengerTraveler traveler : travelers) {
            String maskedIdNo = maskIdNo(traveler.getIdNo());
            String maskedPhone = maskPhone(traveler.getPhone());
            SearchResultItemResponse item = base("TRAVELER-" + traveler.getId(),
                    traveler.getPassengerName(),
                    "user " + traveler.getUserId() + " | " + enumName(traveler.getIdType()) + " | " + maskedIdNo + " | " + maskedPhone,
                    traveler.isDefaultTraveler() ? "DEFAULT" : "NORMAL",
                    "TRAVELER",
                    String.valueOf(traveler.getId()),
                    traveler.getUpdatedAt());
            item.setMatchedFields(matched(keyword, "passengerName", traveler.getPassengerName(),
                    "maskedIdNo", maskedIdNo,
                    "maskedPhone", maskedPhone,
                    "userId", String.valueOf(traveler.getUserId())));
            if (includeTrace) {
                item.getTrace().add("TRAVELER -> PASSENGER_PROFILE");
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapRisks(List<RiskEvent> risks, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (RiskEvent risk : risks) {
            TicketOrder order = risk.getOrder();
            SearchResultItemResponse item = base("RISK-" + risk.getId(),
                    enumName(risk.getRiskType()) + " / " + enumName(risk.getRiskLevel()),
                    (order == null ? "order -" : order.getOrderNo()) + " | " + risk.getReason(),
                    enumName(risk.getStatus()),
                    "RISK",
                    String.valueOf(risk.getId()),
                    risk.getCreatedAt());
            if (order != null) {
                item.setOrderId(order.getId());
                item.setOrderNo(order.getOrderNo());
                item.setDetailAction("ORDER_DETAIL");
            }
            item.setMatchedFields(matched(keyword, "riskType", enumName(risk.getRiskType()),
                    "riskStatus", enumName(risk.getStatus()),
                    "scene", enumName(risk.getScene()),
                    "reason", risk.getReason(),
                    "orderNo", order == null ? null : order.getOrderNo(),
                    "userId", String.valueOf(risk.getUserId())));
            if (includeTrace) {
                item.getTrace().add("RISK -> ORDER_DETAIL");
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapOutbox(List<OutboxEvent> events, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (OutboxEvent event : events) {
            SearchResultItemResponse item = base("OUTBOX-" + event.getId(),
                    event.getEventType(),
                    event.getAggregateType() + " | " + event.getAggregateId(),
                    enumName(event.getStatus()),
                    "OUTBOX",
                    event.getEventId(),
                    event.getCreatedAt());
            item.setMatchedFields(matched(keyword, "eventId", event.getEventId(),
                    "eventType", event.getEventType(),
                    "aggregateType", event.getAggregateType(),
                    "aggregateId", event.getAggregateId(),
                    "payload", event.getPayload()));
            Long orderId = inferOrderId(event.getAggregateType(), event.getAggregateId());
            item.setOrderId(orderId);
            if (orderId != null) {
                item.setDetailAction("ORDER_DETAIL");
            }
            if (includeTrace) {
                item.getTrace().add("OUTBOX -> " + event.getAggregateType());
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapNotifications(List<NotificationRecord> notifications, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (NotificationRecord notification : notifications) {
            SearchResultItemResponse item = base("NOTIFICATION-" + notification.getId(),
                    notification.getTitle(),
                    notification.getNotificationNo() + " | " + notification.getContent(),
                    enumName(notification.getStatus()),
                    "NOTIFICATION",
                    notification.getNotificationNo(),
                    notification.getCreatedAt());
            item.setOrderId(notification.getOrderId());
            item.setOrderNo(notification.getOrderNo());
            item.setTicketNo(notification.getTicketNo());
            item.setPaymentNo(notification.getPaymentNo());
            item.setRefundNo(notification.getRefundNo());
            item.setNotificationNo(notification.getNotificationNo());
            if (notification.getOrderId() != null) {
                item.setDetailAction("ORDER_DETAIL");
            }
            item.setMatchedFields(matched(keyword, "notificationNo", notification.getNotificationNo(),
                    "type", enumName(notification.getType()),
                    "title", notification.getTitle(),
                    "orderNo", notification.getOrderNo(),
                    "ticketNo", notification.getTicketNo(),
                    "paymentNo", notification.getPaymentNo(),
                    "refundNo", notification.getRefundNo()));
            if (includeTrace) {
                item.getTrace().add("NOTIFICATION -> " + notification.getBusinessType());
            }
            items.add(item);
        }
        return items;
    }

    private List<SearchResultItemResponse> mapOperationLogs(List<OperationLog> logs, String keyword, boolean includeTrace) {
        List<SearchResultItemResponse> items = new ArrayList<SearchResultItemResponse>();
        for (OperationLog log : logs) {
            SearchResultItemResponse item = base("LOG-" + log.getId(),
                    log.getAction(),
                    log.getOperator() + " | " + log.getTargetType() + " | " + safe(log.getDetail()),
                    null,
                    "OPERATION_LOG",
                    String.valueOf(log.getId()),
                    log.getCreatedAt());
            Long orderId = inferOrderId(log.getTargetType(), log.getTargetId());
            item.setOrderId(orderId);
            if (orderId != null) {
                item.setDetailAction("ORDER_DETAIL");
            }
            item.setMatchedFields(matched(keyword, "operator", log.getOperator(),
                    "action", log.getAction(),
                    "targetType", log.getTargetType(),
                    "targetId", log.getTargetId(),
                    "detail", log.getDetail()));
            if (includeTrace) {
                item.getTrace().add("LOG -> " + log.getTargetType());
            }
            items.add(item);
        }
        return items;
    }

    private SearchResultGroupResponse group(SearchType type, List<SearchResultItemResponse> items) {
        SearchResultGroupResponse group = new SearchResultGroupResponse();
        group.setType(type.name());
        group.setTypeName(typeName(type));
        group.setItems(items);
        group.setCount(items.size());
        return group;
    }

    private SearchResultItemResponse base(String id,
                                          String title,
                                          String subtitle,
                                          String status,
                                          String businessType,
                                          String businessId,
                                          LocalDateTime createdAt) {
        SearchResultItemResponse item = new SearchResultItemResponse();
        item.setId(id);
        item.setTitle(title);
        item.setSubtitle(subtitle);
        item.setStatus(status);
        item.setBusinessType(businessType);
        item.setBusinessId(businessId);
        item.setCreatedAt(createdAt);
        return item;
    }

    private String normalizeKeyword(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.isEmpty()) {
            throw new BusinessException("Search keyword is required");
        }
        if (value.length() < 2) {
            throw new BusinessException("Search keyword must contain at least 2 characters");
        }
        return value;
    }

    private int normalizeLimit(Integer limitPerType) {
        if (limitPerType == null) {
            return DEFAULT_LIMIT_PER_TYPE;
        }
        if (limitPerType < 1) {
            return DEFAULT_LIMIT_PER_TYPE;
        }
        return Math.min(limitPerType, MAX_LIMIT_PER_TYPE);
    }

    private Set<SearchType> parseTypes(String types) {
        if (types == null || types.trim().isEmpty()) {
            return EnumSet.allOf(SearchType.class);
        }
        EnumSet<SearchType> result = EnumSet.noneOf(SearchType.class);
        String[] tokens = types.split(",");
        for (String token : tokens) {
            String value = token.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                result.add(SearchType.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException("Unsupported search type: " + value);
            }
        }
        if (result.isEmpty()) {
            return EnumSet.allOf(SearchType.class);
        }
        return result;
    }

    private List<String> matched(String keyword, String... pairs) {
        List<String> fields = new ArrayList<String>();
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String field = pairs[i];
            String value = pairs[i + 1];
            if (value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword)) {
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            fields.add("related");
        }
        return fields;
    }

    private String safeTrainNo(TicketOrder order) {
        return order.getTrain() == null ? "-" : order.getTrain().getTrainNo();
    }

    private String safeRoute(TicketOrder order) {
        if (order.getTrain() == null) {
            return "-";
        }
        String departure = order.getTrain().getDepartureStation() == null ? "-" : order.getTrain().getDepartureStation().getName();
        String arrival = order.getTrain().getArrivalStation() == null ? "-" : order.getTrain().getArrivalStation().getName();
        return departure + " -> " + arrival;
    }

    private String typeName(SearchType type) {
        if (SearchType.ORDER.equals(type)) {
            return "Order";
        }
        if (SearchType.TICKET.equals(type)) {
            return "Ticket";
        }
        if (SearchType.PAYMENT.equals(type)) {
            return "Payment";
        }
        if (SearchType.REFUND.equals(type)) {
            return "Refund";
        }
        if (SearchType.TRAVELER.equals(type)) {
            return "Traveler";
        }
        if (SearchType.RISK.equals(type)) {
            return "Risk";
        }
        if (SearchType.OUTBOX.equals(type)) {
            return "Outbox";
        }
        if (SearchType.NOTIFICATION.equals(type)) {
            return "Notification";
        }
        return "Operation Log";
    }

    private Long inferOrderId(String targetType, String targetId) {
        if (targetType == null || targetId == null || !isNumeric(targetId)) {
            return null;
        }
        String normalized = targetType.toUpperCase(Locale.ROOT);
        if ("ORDER".equals(normalized) || "TICKET_ORDER".equals(normalized)) {
            return Long.valueOf(targetId);
        }
        return null;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String maskIdNo(String value) {
        if (value == null || value.length() <= 8) {
            return value == null ? "-" : "****";
        }
        return value.substring(0, 4) + "********" + value.substring(value.length() - 4);
    }

    private String maskPhone(String value) {
        if (value == null || value.length() < 7) {
            return value == null ? "-" : "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
