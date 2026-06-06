package com.example.railway.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.PassengerIdType;
import com.example.railway.domain.PassengerTraveler;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.UserRole;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.OrderDetailResponse;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.PassengerCreateOrderRequest;
import com.example.railway.dto.PassengerSummaryResponse;
import com.example.railway.dto.PassengerTravelerRequest;
import com.example.railway.dto.PassengerTravelerResponse;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.repository.PassengerTravelerRepository;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.security.AuthContext;
import com.example.railway.security.AuthPrincipal;
import com.example.railway.security.AuthorizationException;

@Service
public class PassengerService {

    private final TicketOrderRepository ticketOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final RefundService refundService;
    private final OrderDetailService orderDetailService;
    private final PassengerTravelerRepository passengerTravelerRepository;

    public PassengerService(TicketOrderRepository ticketOrderRepository,
                            PaymentRecordRepository paymentRecordRepository,
                            RefundRecordRepository refundRecordRepository,
                            OrderService orderService,
                            PaymentService paymentService,
                            RefundService refundService,
                            OrderDetailService orderDetailService,
                            PassengerTravelerRepository passengerTravelerRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.orderDetailService = orderDetailService;
        this.passengerTravelerRepository = passengerTravelerRepository;
    }

    @Transactional(readOnly = true)
    public PassengerSummaryResponse summary() {
        Long userId = currentUserId();
        PassengerSummaryResponse response = new PassengerSummaryResponse();
        response.setPendingPaymentOrderCount(ticketOrderRepository.countByUserIdAndStatus(userId, OrderStatus.PENDING_PAYMENT));
        response.setPaidOrderCount(ticketOrderRepository.countByUserIdAndStatus(userId, OrderStatus.PAID));
        response.setClosedOrderCount(ticketOrderRepository.countByUserIdAndStatus(userId, OrderStatus.CLOSED));
        response.setRefundedOrderCount(ticketOrderRepository.countByUserIdAndStatus(userId, OrderStatus.REFUNDED));
        response.setPaymentCount(paymentRecordRepository.countByUserId(userId));
        response.setRefundCount(refundRecordRepository.countByUserId(userId));
        response.setLatestOrders(toOrderResponses(ticketOrderRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)));
        response.setUpcomingTrips(toOrderResponses(ticketOrderRepository.findTop5ByUserIdAndStatusAndTravelDateGreaterThanEqualOrderByTravelDateAscCreatedAtDesc(
                userId,
                OrderStatus.PAID,
                LocalDate.now()
        )));
        return response;
    }

    @Transactional(readOnly = true)
    public OrderPageResponse listOrders(String status, Integer page, Integer size) {
        return orderService.listOrders(currentUserId(), status, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse orderDetail(Long orderId) {
        return orderDetailService.passengerDetail(orderId, currentUserId());
    }

    @Transactional(readOnly = true)
    public List<PassengerTravelerResponse> listTravelers() {
        List<PassengerTravelerResponse> responses = new ArrayList<PassengerTravelerResponse>();
        for (PassengerTraveler traveler : passengerTravelerRepository.findByUserIdOrderByDefaultTravelerDescUpdatedAtDesc(currentUserId())) {
            responses.add(PassengerTravelerResponse.from(traveler));
        }
        return responses;
    }

    @Transactional
    public PassengerTravelerResponse createTraveler(PassengerTravelerRequest request) {
        Long userId = currentUserId();
        PassengerTraveler traveler = new PassengerTraveler();
        applyTravelerRequest(traveler, userId, request, null);
        LocalDateTime now = LocalDateTime.now();
        traveler.setCreatedAt(now);
        traveler.setUpdatedAt(now);
        PassengerTraveler saved = passengerTravelerRepository.save(traveler);
        return PassengerTravelerResponse.from(saved);
    }

    @Transactional
    public PassengerTravelerResponse updateTraveler(Long travelerId, PassengerTravelerRequest request) {
        Long userId = currentUserId();
        PassengerTraveler traveler = passengerTravelerRepository.findByIdAndUserId(travelerId, userId)
                .orElseThrow(() -> new AuthorizationException("Only current passenger traveler can be updated"));
        applyTravelerRequest(traveler, userId, request, travelerId);
        traveler.setUpdatedAt(LocalDateTime.now());
        PassengerTraveler saved = passengerTravelerRepository.save(traveler);
        return PassengerTravelerResponse.from(saved);
    }

    @Transactional
    public void deleteTraveler(Long travelerId) {
        Long userId = currentUserId();
        PassengerTraveler traveler = passengerTravelerRepository.findByIdAndUserId(travelerId, userId)
                .orElseThrow(() -> new AuthorizationException("Only current passenger traveler can be deleted"));
        passengerTravelerRepository.delete(traveler);
    }

    @Transactional
    public PassengerTravelerResponse setDefaultTraveler(Long travelerId) {
        Long userId = currentUserId();
        PassengerTraveler target = passengerTravelerRepository.findByIdAndUserId(travelerId, userId)
                .orElseThrow(() -> new AuthorizationException("Only current passenger traveler can be set as default"));
        for (PassengerTraveler traveler : passengerTravelerRepository.findByUserIdOrderByDefaultTravelerDescUpdatedAtDesc(userId)) {
            traveler.setDefaultTraveler(traveler.getId().equals(target.getId()));
            traveler.setUpdatedAt(LocalDateTime.now());
            passengerTravelerRepository.save(traveler);
        }
        return PassengerTravelerResponse.from(passengerTravelerRepository.findByIdAndUserId(travelerId, userId).orElse(target));
    }

    @Transactional
    public OrderResponse createOrder(PassengerCreateOrderRequest request) {
        AuthPrincipal principal = currentUser();
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(principal.getUserId());
        createOrderRequest.setRequestId(request.getRequestId());
        createOrderRequest.setTrainId(request.getTrainId());
        createOrderRequest.setInventoryId(request.getInventoryId());
        applyTravelerToOrderRequest(request, createOrderRequest, principal.getUserId());
        return orderService.createOrder(createOrderRequest);
    }

    @Transactional
    public OrderResponse payOrder(Long orderId) {
        Long userId = currentUserId();
        TicketOrder order = ownedOrder(orderId, userId);
        if (OrderStatus.PAID.equals(order.getStatus())) {
            return OrderResponse.from(order);
        }
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new BusinessException("Current order status does not allow payment");
        }

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(order.getId());
        PaymentResponse payment = paymentService.createPayment(paymentRequest);
        paymentService.handleCallback(paymentService.buildMockCallback(
                payment.getPaymentNo(),
                "passenger-payment-callback-" + payment.getPaymentNo(),
                true,
                "CH_PASS_" + payment.getPaymentNo(),
                "passenger mock payment success"
        ));
        return OrderResponse.from(ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found")));
    }

    @Transactional
    public OrderResponse closeOrder(Long orderId) {
        ownedOrder(orderId, currentUserId());
        return orderService.closeUnpaidOrder(orderId);
    }

    @Transactional
    public OrderResponse refundOrder(Long orderId) {
        ownedOrder(orderId, currentUserId());
        return orderService.refund(orderId);
    }

    @Transactional(readOnly = true)
    public PaymentPageResponse listPayments(String status, Integer page, Integer size) {
        return paymentService.listPaymentsForUser(currentUserId(), status, page, size);
    }

    @Transactional(readOnly = true)
    public RefundPageResponse listRefunds(String status, Integer page, Integer size) {
        return refundService.listRefundsForUser(currentUserId(), status, page, size);
    }

    private TicketOrder ownedOrder(Long orderId, Long userId) {
        return ticketOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AuthorizationException("Only current passenger order can be operated"));
    }

    private Long currentUserId() {
        return currentUser().getUserId();
    }

    private AuthPrincipal currentUser() {
        AuthPrincipal principal = AuthContext.current();
        if (!UserRole.USER.equals(principal.getRole())) {
            throw new AuthorizationException("Only passenger users can access this API");
        }
        return principal;
    }

    private void applyTravelerToOrderRequest(PassengerCreateOrderRequest request,
                                             CreateOrderRequest createOrderRequest,
                                             Long userId) {
        if (request.getTravelerId() != null) {
            PassengerTraveler traveler = passengerTravelerRepository.findByIdAndUserId(request.getTravelerId(), userId)
                    .orElseThrow(() -> new AuthorizationException("Only current passenger traveler can be used to create order"));
            createOrderRequest.setPassengerName(traveler.getPassengerName());
            createOrderRequest.setPassengerIdCard(traveler.getIdNo());
            createOrderRequest.setPassengerIdType(traveler.getIdType().name());
            createOrderRequest.setPassengerPhone(traveler.getPhone());
            return;
        }
        if (isBlank(request.getPassengerName()) || isBlank(request.getPassengerIdCard())) {
            throw new BusinessException("Passenger name and id number are required when travelerId is not provided");
        }
        createOrderRequest.setPassengerName(request.getPassengerName().trim());
        createOrderRequest.setPassengerIdCard(request.getPassengerIdCard().trim());
        createOrderRequest.setPassengerIdType(isBlank(request.getPassengerIdType()) ? PassengerIdType.ID_CARD.name() : request.getPassengerIdType().trim());
        createOrderRequest.setPassengerPhone(isBlank(request.getPassengerPhone()) ? null : request.getPassengerPhone().trim());
    }

    private void applyTravelerRequest(PassengerTraveler traveler, Long userId, PassengerTravelerRequest request, Long currentId) {
        String name = normalizeRequired(request.getPassengerName(), "Passenger name is required");
        PassengerIdType idType = parseIdType(request.getIdType());
        String idNo = normalizeRequired(request.getIdNo(), "Passenger id number is required");
        String phone = isBlank(request.getPhone()) ? null : request.getPhone().trim();
        boolean duplicated = currentId == null
                ? passengerTravelerRepository.existsByUserIdAndPassengerNameAndIdTypeAndIdNo(userId, name, idType, idNo)
                : passengerTravelerRepository.existsByUserIdAndPassengerNameAndIdTypeAndIdNoAndIdNot(userId, name, idType, idNo, currentId);
        if (duplicated) {
            throw new BusinessException("Passenger traveler already exists");
        }
        if (Boolean.TRUE.equals(request.getDefaultTraveler())) {
            for (PassengerTraveler existing : passengerTravelerRepository.findByUserIdOrderByDefaultTravelerDescUpdatedAtDesc(userId)) {
                if (currentId == null || !existing.getId().equals(currentId)) {
                    existing.setDefaultTraveler(false);
                    existing.setUpdatedAt(LocalDateTime.now());
                    passengerTravelerRepository.save(existing);
                }
            }
            traveler.setDefaultTraveler(true);
        } else if (currentId == null && !passengerTravelerRepository.findByUserIdAndDefaultTravelerTrue(userId).isPresent()) {
            traveler.setDefaultTraveler(true);
        } else if (!Boolean.TRUE.equals(request.getDefaultTraveler())) {
            traveler.setDefaultTraveler(false);
        }
        traveler.setUserId(userId);
        traveler.setPassengerName(name);
        traveler.setIdType(idType);
        traveler.setIdNo(idNo);
        traveler.setPhone(phone);
    }

    private PassengerIdType parseIdType(String value) {
        if (isBlank(value)) {
            return PassengerIdType.ID_CARD;
        }
        try {
            return PassengerIdType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unsupported passenger id type: " + value);
        }
    }

    private String normalizeRequired(String value, String message) {
        if (isBlank(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<OrderResponse> toOrderResponses(List<TicketOrder> orders) {
        List<OrderResponse> responses = new ArrayList<OrderResponse>();
        for (TicketOrder order : orders) {
            responses.add(OrderResponse.from(order));
        }
        return responses;
    }
}
