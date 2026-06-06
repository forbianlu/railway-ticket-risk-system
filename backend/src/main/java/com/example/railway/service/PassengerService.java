package com.example.railway.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.UserRole;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.OrderDetailResponse;
import com.example.railway.dto.PassengerCreateOrderRequest;
import com.example.railway.dto.PassengerSummaryResponse;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.RefundPageResponse;
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

    public PassengerService(TicketOrderRepository ticketOrderRepository,
                            PaymentRecordRepository paymentRecordRepository,
                            RefundRecordRepository refundRecordRepository,
                            OrderService orderService,
                            PaymentService paymentService,
                            RefundService refundService,
                            OrderDetailService orderDetailService) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.refundService = refundService;
        this.orderDetailService = orderDetailService;
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

    @Transactional
    public OrderResponse createOrder(PassengerCreateOrderRequest request) {
        AuthPrincipal principal = currentUser();
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(principal.getUserId());
        createOrderRequest.setRequestId(request.getRequestId());
        createOrderRequest.setTrainId(request.getTrainId());
        createOrderRequest.setInventoryId(request.getInventoryId());
        createOrderRequest.setPassengerName(request.getPassengerName());
        createOrderRequest.setPassengerIdCard(request.getPassengerIdCard());
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
            throw new BusinessException("当前订单状态不允许支付");
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
                .orElseThrow(() -> new BusinessException("订单不存在")));
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
                .orElseThrow(() -> new AuthorizationException("只能操作当前乘客自己的订单"));
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

    private List<OrderResponse> toOrderResponses(List<TicketOrder> orders) {
        List<OrderResponse> responses = new ArrayList<OrderResponse>();
        for (TicketOrder order : orders) {
            responses.add(OrderResponse.from(order));
        }
        return responses;
    }
}
