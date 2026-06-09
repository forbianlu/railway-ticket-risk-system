package com.example.railway.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.NotificationRecord;
import com.example.railway.domain.NotificationStatus;
import com.example.railway.domain.NotificationType;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.TicketRecord;
import com.example.railway.dto.NotificationPageResponse;
import com.example.railway.dto.NotificationResponse;
import com.example.railway.dto.NotificationSummaryResponse;
import com.example.railway.repository.NotificationRecordRepository;
import com.example.railway.security.AuthorizationException;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@Service
public class NotificationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter NUMBER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final NotificationRecordRepository notificationRecordRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    public NotificationService(NotificationRecordRepository notificationRecordRepository,
                               OutboxEventPublisher outboxEventPublisher) {
        this.notificationRecordRepository = notificationRecordRepository;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    public void notifyOrderCreated(TicketOrder order) {
        createSafely(NotificationType.ORDER_CREATED,
                "ORDER",
                String.valueOf(order.getId()),
                order.getUserId(),
                "Order created",
                "Order " + order.getOrderNo() + " is pending payment. Amount " + order.getAmount()
                        + ", payment deadline " + order.getPaymentDeadlineAt() + ".",
                order.getId(),
                order.getOrderNo(),
                null,
                null,
                null);
    }

    public void notifyPaymentSucceeded(PaymentRecord payment) {
        createSafely(NotificationType.PAYMENT_SUCCEEDED,
                "PAYMENT",
                payment.getPaymentNo(),
                payment.getUserId(),
                "Payment confirmed",
                "Order " + payment.getOrderNo() + " payment succeeded. Amount " + payment.getAmount()
                        + ", paymentNo " + payment.getPaymentNo() + ".",
                payment.getOrderId(),
                payment.getOrderNo(),
                null,
                payment.getPaymentNo(),
                null);
    }

    public void notifyTicketIssued(TicketRecord ticket) {
        createSafely(NotificationType.TICKET_ISSUED,
                "TICKET",
                String.valueOf(ticket.getId()),
                ticket.getUserId(),
                "Ticket issued",
                "Ticket " + ticket.getTicketNo() + " for train " + ticket.getTrainNo() + " "
                        + ticket.getDepartureStation() + " to " + ticket.getArrivalStation()
                        + " on " + ticket.getTravelDate() + " has been issued.",
                ticket.getOrderId(),
                ticket.getOrderNo(),
                ticket.getTicketNo(),
                null,
                null);
    }

    public void notifyOrderClosed(TicketOrder order, String reason) {
        createSafely(NotificationType.ORDER_CLOSED,
                "ORDER",
                String.valueOf(order.getId()),
                order.getUserId(),
                "Order closed",
                "Order " + order.getOrderNo() + " has been closed. Reason: " + normalizeText(reason, "pending payment closed") + ".",
                order.getId(),
                order.getOrderNo(),
                null,
                null,
                null);
    }

    public void notifyOrderRefunded(TicketOrder order, RefundRecord refund) {
        createSafely(NotificationType.ORDER_REFUNDED,
                "ORDER",
                String.valueOf(order.getId()),
                order.getUserId(),
                "Refund requested",
                "Order " + order.getOrderNo() + " has entered refund processing. Refund status "
                        + (refund == null || refund.getStatus() == null ? "PENDING" : refund.getStatus().name()) + ".",
                order.getId(),
                order.getOrderNo(),
                null,
                refund == null ? null : refund.getPaymentNo(),
                refund == null ? null : refund.getRefundNo());
    }

    public void notifyRefundSucceeded(RefundRecord refund) {
        createSafely(NotificationType.REFUND_SUCCEEDED,
                "REFUND",
                refund.getRefundNo(),
                refund.getUserId(),
                "Refund succeeded",
                "Refund " + refund.getRefundNo() + " succeeded. Amount " + refund.getAmount() + ".",
                refund.getOrderId(),
                refund.getOrderNo(),
                null,
                refund.getPaymentNo(),
                refund.getRefundNo());
    }

    public void notifyRefundFailed(RefundRecord refund) {
        createSafely(NotificationType.REFUND_FAILED,
                "REFUND",
                refund.getRefundNo(),
                refund.getUserId(),
                "Refund failed",
                "Refund " + refund.getRefundNo() + " failed. Amount " + refund.getAmount() + ".",
                refund.getOrderId(),
                refund.getOrderNo(),
                null,
                refund.getPaymentNo(),
                refund.getRefundNo());
    }

    @Transactional
    public NotificationRecord createNotification(NotificationType type,
                                                 String businessType,
                                                 String businessId,
                                                 Long userId,
                                                 String title,
                                                 String content,
                                                 Long orderId,
                                                 String orderNo,
                                                 String ticketNo,
                                                 String paymentNo,
                                                 String refundNo) {
        String normalizedBusinessType = normalizeRequired(businessType, "businessType is required");
        String normalizedBusinessId = normalizeRequired(businessId, "businessId is required");
        if (notificationRecordRepository.existsByUserIdAndTypeAndBusinessTypeAndBusinessId(
                userId,
                type,
                normalizedBusinessType,
                normalizedBusinessId
        )) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        NotificationRecord record = new NotificationRecord();
        record.setNotificationNo(generateNotificationNo());
        record.setUserId(userId);
        record.setTitle(normalizeRequired(title, "title is required"));
        record.setContent(normalizeRequired(content, "content is required"));
        record.setType(type);
        record.setStatus(NotificationStatus.UNREAD);
        record.setBusinessType(normalizedBusinessType);
        record.setBusinessId(normalizedBusinessId);
        record.setOrderId(orderId);
        record.setOrderNo(orderNo);
        record.setTicketNo(ticketNo);
        record.setPaymentNo(paymentNo);
        record.setRefundNo(refundNo);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        NotificationRecord saved = notificationRecordRepository.save(record);
        publishNotificationEvent(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse listPassengerNotifications(Long userId, String status, Integer page, Integer size) {
        return NotificationPageResponse.from(listInternal(userId, status, null, null, null, null, null, page, size));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRecordRepository.countByUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long id) {
        NotificationRecord record = notificationRecordRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AuthorizationException("Only current passenger notification can be marked as read"));
        if (!NotificationStatus.READ.equals(record.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            record.setStatus(NotificationStatus.READ);
            record.setReadAt(now);
            record.setUpdatedAt(now);
            record = notificationRecordRepository.save(record);
        }
        return NotificationResponse.from(record);
    }

    @Transactional
    public long markAllAsRead(Long userId) {
        List<NotificationRecord> records = notificationRecordRepository.findByUserId(userId);
        long changed = 0;
        LocalDateTime now = LocalDateTime.now();
        for (NotificationRecord record : records) {
            if (NotificationStatus.UNREAD.equals(record.getStatus())) {
                record.setStatus(NotificationStatus.READ);
                record.setReadAt(now);
                record.setUpdatedAt(now);
                notificationRecordRepository.save(record);
                changed++;
            }
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse passengerSummary(Long userId) {
        return buildSummary(notificationRecordRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse adminListNotifications(Long userId,
                                                           String status,
                                                           String type,
                                                           String businessType,
                                                           String orderNo,
                                                           LocalDateTime fromDate,
                                                           LocalDateTime toDate,
                                                           Integer page,
                                                           Integer size) {
        return NotificationPageResponse.from(listInternal(userId, status, type, businessType, orderNo, fromDate, toDate, page, size));
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse adminSummary() {
        return buildSummary(notificationRecordRepository.findAll());
    }

    private void createSafely(NotificationType type,
                              String businessType,
                              String businessId,
                              Long userId,
                              String title,
                              String content,
                              Long orderId,
                              String orderNo,
                              String ticketNo,
                              String paymentNo,
                              String refundNo) {
        try {
            createNotification(type, businessType, businessId, userId, title, content, orderId, orderNo, ticketNo, paymentNo, refundNo);
        } catch (RuntimeException ignored) {
            // Notifications should not block the core ticket transaction.
        }
    }

    private Page<NotificationRecord> listInternal(Long userId,
                                                  String status,
                                                  String type,
                                                  String businessType,
                                                  String orderNo,
                                                  LocalDateTime fromDate,
                                                  LocalDateTime toDate,
                                                  Integer page,
                                                  Integer size) {
        final NotificationStatus notificationStatus = parseStatus(status);
        final NotificationType notificationType = parseType(type);
        final String normalizedBusinessType = normalizeText(businessType, null);
        final String normalizedOrderNo = normalizeText(orderNo, null);
        if (fromDate != null && toDate != null && !fromDate.isBefore(toDate)) {
            throw new BusinessException("Invalid date range");
        }
        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );
        return notificationRecordRepository.findAll(
                buildSpecification(userId, notificationStatus, notificationType, normalizedBusinessType, normalizedOrderNo, fromDate, toDate),
                pageRequest
        );
    }

    private Specification<NotificationRecord> buildSpecification(final Long userId,
                                                                 final NotificationStatus status,
                                                                 final NotificationType type,
                                                                 final String businessType,
                                                                 final String orderNo,
                                                                 final LocalDateTime fromDate,
                                                                 final LocalDateTime toDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<Predicate>();
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            if (businessType != null) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.<String>get("businessType")), businessType.toLowerCase()));
            }
            if (orderNo != null) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.<String>get("orderNo")), "%" + orderNo.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThan(root.<LocalDateTime>get("createdAt"), toDate));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private NotificationSummaryResponse buildSummary(List<NotificationRecord> records) {
        NotificationSummaryResponse response = new NotificationSummaryResponse();
        Map<String, Long> totalByType = new LinkedHashMap<String, Long>();
        Map<String, Long> unreadByType = new LinkedHashMap<String, Long>();
        Map<String, Long> byStatus = new LinkedHashMap<String, Long>();
        long unread = 0;
        LocalDateTime latest = null;
        for (NotificationRecord record : records) {
            String type = record.getType() == null ? "UNKNOWN" : record.getType().name();
            String status = record.getStatus() == null ? "UNKNOWN" : record.getStatus().name();
            totalByType.put(type, totalByType.containsKey(type) ? totalByType.get(type) + 1 : 1);
            byStatus.put(status, byStatus.containsKey(status) ? byStatus.get(status) + 1 : 1);
            if (NotificationStatus.UNREAD.equals(record.getStatus())) {
                unread++;
                unreadByType.put(type, unreadByType.containsKey(type) ? unreadByType.get(type) + 1 : 1);
            }
            if (record.getCreatedAt() != null && (latest == null || record.getCreatedAt().isAfter(latest))) {
                latest = record.getCreatedAt();
            }
        }
        response.setTotalCount(records.size());
        response.setUnreadCount(unread);
        response.setReadCount(records.size() - unread);
        response.setTotalCountByType(totalByType);
        response.setUnreadCountByType(unreadByType);
        response.setCountByType(totalByType);
        response.setCountByStatus(byStatus);
        response.setLatestCreatedAt(latest);
        return response;
    }

    private void publishNotificationEvent(NotificationRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("notificationNo", record.getNotificationNo());
        payload.put("userId", record.getUserId());
        payload.put("type", record.getType() == null ? null : record.getType().name());
        payload.put("businessType", record.getBusinessType());
        payload.put("businessId", record.getBusinessId());
        outboxEventPublisher.publish(OutboxEventTypes.NOTIFICATION_CREATED, "NOTIFICATION", record.getNotificationNo(), payload);
    }

    private String generateNotificationNo() {
        String notificationNo;
        do {
            String timestamp = LocalDateTime.now().format(NUMBER_FORMATTER);
            int random = ThreadLocalRandom.current().nextInt(1000, 10000);
            notificationNo = "NT" + timestamp + random;
        } while (notificationRecordRepository.existsByNotificationNo(notificationNo));
        return notificationNo;
    }

    private NotificationStatus parseStatus(String status) {
        String normalized = normalizeText(status, null);
        if (normalized == null) {
            return null;
        }
        try {
            return NotificationStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid notification status: " + status);
        }
    }

    private NotificationType parseType(String type) {
        String normalized = normalizeText(type, null);
        if (normalized == null) {
            return null;
        }
        try {
            return NotificationType.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid notification type: " + type);
        }
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 0) {
            throw new BusinessException("Page cannot be less than 0");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size <= 0) {
            throw new BusinessException("Page size must be positive");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value, null);
        if (normalized == null) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
