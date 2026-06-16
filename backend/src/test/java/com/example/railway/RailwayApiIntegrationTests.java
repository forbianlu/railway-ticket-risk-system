package com.example.railway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.railway.bootstrap.DemoDataInitializer;
import com.example.railway.config.RateLimitProperties;
import com.example.railway.config.PaymentCallbackProperties;
import com.example.railway.config.RefundCallbackProperties;
import com.example.railway.config.TrainSearchCacheProperties;
import com.example.railway.domain.OperationLog;
import com.example.railway.domain.NotificationRecord;
import com.example.railway.domain.NotificationStatus;
import com.example.railway.domain.NotificationType;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.PaymentStatus;
import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.RefundStatus;
import com.example.railway.domain.RiskStatus;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Station;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.TicketRecord;
import com.example.railway.domain.TicketChangeRecord;
import com.example.railway.domain.TicketStatus;
import com.example.railway.domain.Train;
import com.example.railway.dto.AuthResponse;
import com.example.railway.dto.AdminGlobalSearchResponse;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.DashboardSummary;
import com.example.railway.dto.LoginRequest;
import com.example.railway.dto.NotificationPageResponse;
import com.example.railway.dto.NotificationResponse;
import com.example.railway.dto.NotificationSummaryResponse;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.OrderDetailResponse;
import com.example.railway.dto.OutboxDispatchResponse;
import com.example.railway.dto.OutboxEventPageResponse;
import com.example.railway.dto.OutboxEventResponse;
import com.example.railway.dto.OutboxEventSummaryResponse;
import com.example.railway.dto.OutboxRetryResponse;
import com.example.railway.dto.PassengerCreateOrderRequest;
import com.example.railway.dto.PassengerChangeTicketRequest;
import com.example.railway.dto.PassengerSummaryResponse;
import com.example.railway.dto.PassengerTodoItemResponse;
import com.example.railway.dto.PassengerTransactionSummaryResponse;
import com.example.railway.dto.PassengerTravelerRequest;
import com.example.railway.dto.PassengerTravelerResponse;
import com.example.railway.dto.PaymentCallbackRequest;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.RateLimitSummary;
import com.example.railway.dto.RefundCallbackRequest;
import com.example.railway.dto.RefundPageResponse;
import com.example.railway.dto.RefundResponse;
import com.example.railway.dto.RiskEventHandleRecordResponse;
import com.example.railway.dto.RiskEventPageResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.RiskHandleRequest;
import com.example.railway.dto.SearchResultGroupResponse;
import com.example.railway.dto.SearchResultItemResponse;
import com.example.railway.dto.RiskSummaryResponse;
import com.example.railway.dto.TicketPageResponse;
import com.example.railway.dto.TicketChangePageResponse;
import com.example.railway.dto.TicketChangeResponse;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.repository.AppUserRepository;
import com.example.railway.repository.OperationLogRepository;
import com.example.railway.repository.NotificationRecordRepository;
import com.example.railway.repository.OutboxEventRepository;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.StationRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.repository.TicketRecordRepository;
import com.example.railway.repository.TicketChangeRecordRepository;
import com.example.railway.repository.TrainRepository;
import com.example.railway.service.CallbackSignatureService;
import com.example.railway.service.outbox.OutboxEventDispatcher;
import com.example.railway.service.outbox.OutboxEventPublisher;
import com.example.railway.service.outbox.OutboxEventTypes;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RailwayApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private SeatInventoryRepository seatInventoryRepository;

    @Autowired
    private TicketOrderRepository ticketOrderRepository;

    @Autowired
    private TicketRecordRepository ticketRecordRepository;

    @Autowired
    private TicketChangeRecordRepository ticketChangeRecordRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private RefundRecordRepository refundRecordRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Autowired
    private TrainSearchCacheProperties trainSearchCacheProperties;

    @Autowired
    private CallbackSignatureService callbackSignatureService;

    @Autowired
    private PaymentCallbackProperties paymentCallbackProperties;

    @Autowired
    private RefundCallbackProperties refundCallbackProperties;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private OutboxEventDispatcher outboxEventDispatcher;

    @Test
    void shouldSearchTrainInventories() {
        List<TrainSearchResponse> trains = searchToday();

        assertThat(trains).isNotEmpty();
        assertThat(trains)
                .extracting(TrainSearchResponse::getTrainNo)
                .contains("G101");
        assertThat(trains)
                .extracting(TrainSearchResponse::getRemainingSeats)
                .allMatch(remainingSeats -> remainingSeats > 0);
    }

    @Test
    void shouldSeedRichDemoDataIdempotently() throws Exception {
        demoDataInitializer.run();

        long stationCount = stationRepository.count();
        long trainCount = trainRepository.count();
        long inventoryCount = seatInventoryRepository.count();
        long orderCount = ticketOrderRepository.count();
        long ticketCount = ticketRecordRepository.count();
        long paymentCount = paymentRecordRepository.count();
        long refundCount = refundRecordRepository.count();
        long riskCount = riskEventRepository.count();
        long logCount = operationLogRepository.count();
        long notificationCount = notificationRecordRepository.count();
        long outboxCount = outboxEventRepository.count();

        assertThat(stationCount).isGreaterThanOrEqualTo(16);
        assertThat(trainCount).isGreaterThanOrEqualTo(16);
        assertThat(inventoryCount).isGreaterThanOrEqualTo(200);
        assertThat(orderCount).isGreaterThanOrEqualTo(48);
        assertThat(ticketCount).isGreaterThanOrEqualTo(20);
        assertThat(paymentCount).isGreaterThanOrEqualTo(48);
        assertThat(refundCount).isGreaterThanOrEqualTo(12);
        assertThat(riskCount).isGreaterThanOrEqualTo(20);
        assertThat(logCount).isGreaterThanOrEqualTo(30);
        assertThat(notificationCount).isGreaterThanOrEqualTo(12);
        assertThat(outboxCount).isGreaterThanOrEqualTo(10);

        assertThat(ticketOrderRepository.findAll()).extracting(TicketOrder::getStatus)
                .contains(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.CLOSED, OrderStatus.REFUNDED);
        assertThat(ticketRecordRepository.findAll()).extracting(TicketRecord::getStatus)
                .contains(TicketStatus.ISSUED, TicketStatus.REFUNDED);
        assertThat(paymentRecordRepository.findAll()).extracting(PaymentRecord::getStatus)
                .contains(PaymentStatus.PENDING, PaymentStatus.SUCCESS, PaymentStatus.FAILED);
        assertThat(refundRecordRepository.findAll()).extracting(RefundRecord::getStatus)
                .contains(RefundStatus.PENDING, RefundStatus.SUCCESS, RefundStatus.FAILED);
        assertThat(riskEventRepository.findAll()).extracting(event -> event.getStatus())
                .contains(RiskStatus.PENDING, RiskStatus.CONFIRMED, RiskStatus.FALSE_POSITIVE, RiskStatus.CLOSED);
        assertThat(outboxEventRepository.findAll()).extracting(OutboxEvent::getStatus)
                .contains(OutboxEventStatus.PENDING, OutboxEventStatus.DONE, OutboxEventStatus.FAILED);
        assertThat(notificationRecordRepository.findAll()).extracting(NotificationRecord::getStatus)
                .contains(NotificationStatus.UNREAD, NotificationStatus.READ);
        assertThat(notificationRecordRepository.findAll()).extracting(NotificationRecord::getType)
                .contains(NotificationType.ORDER_CREATED, NotificationType.PAYMENT_SUCCEEDED, NotificationType.TICKET_ISSUED);

        demoDataInitializer.run();

        assertThat(stationRepository.count()).isEqualTo(stationCount);
        assertThat(trainRepository.count()).isEqualTo(trainCount);
        assertThat(ticketOrderRepository.count()).isEqualTo(orderCount);
        assertThat(ticketRecordRepository.count()).isEqualTo(ticketCount);
        assertThat(paymentRecordRepository.count()).isEqualTo(paymentCount);
        assertThat(refundRecordRepository.count()).isEqualTo(refundCount);
        assertThat(riskEventRepository.count()).isEqualTo(riskCount);
        assertThat(operationLogRepository.count()).isEqualTo(logCount);
        assertThat(outboxEventRepository.count()).isEqualTo(outboxCount);
    }

    @Test
    void shouldCreateOrderAndListByUser() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3101L;

        OrderResponse created = createOrder(userId, train, "ApiOrderUser");
        TrainSearchResponse afterCreate = findInventory(train.getInventoryId());
        OrderResponse paid = payOrder(created.getId());
        OrderPageResponse orders = fetchOrderPage("/api/orders?userId={userId}", userId);

        assertThat(created.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.getPaymentDeadlineAt()).isNotNull();
        assertThat(afterCreate.getRemainingSeats()).isEqualTo(train.getRemainingSeats() - 1);
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(paid.getPaidAt()).isNotNull();
        assertThat(orders).isNotNull();
        assertThat(orders.getContent())
                .extracting(OrderResponse::getOrderNo)
                .contains(created.getOrderNo());
    }

    @Test
    void shouldFilterAndPaginateOrders() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3120L;

        OrderResponse pending = createOrder(userId, train, "QueryPendingUser");
        OrderResponse paid = createPaidOrder(userId, train, "QueryPaidUser");
        OrderResponse closed = closeOrder(createOrder(userId, train, "QueryClosedUser").getId());
        OrderResponse refunded = refundOrder(createPaidOrder(userId, train, "QueryRefundedUser").getId());

        TicketOrder oldOrder = ticketOrderRepository.findById(pending.getId())
                .orElseThrow(() -> new AssertionError("expected order"));
        oldOrder.setCreatedAt(LocalDateTime.now().minusDays(3));
        ticketOrderRepository.save(oldOrder);

        OrderPageResponse defaultPage = fetchOrderPage("/api/orders?userId={userId}", userId);
        OrderPageResponse firstPage = fetchOrderPage("/api/orders?userId={userId}&page=0&size=2", userId);
        OrderPageResponse secondPage = fetchOrderPage("/api/orders?userId={userId}&page=1&size=2", userId);
        OrderPageResponse pendingPage = fetchOrderPage("/api/orders?userId={userId}&status=PENDING_PAYMENT", userId);
        OrderPageResponse paidPage = fetchOrderPage("/api/orders?userId={userId}&status=PAID", userId);
        OrderPageResponse closedPage = fetchOrderPage("/api/orders?userId={userId}&status=CLOSED", userId);
        OrderPageResponse refundedPage = fetchOrderPage("/api/orders?userId={userId}&status=REFUNDED", userId);
        OrderPageResponse orderNoPage = fetchOrderPage("/api/orders?orderNo={orderNo}", paid.getOrderNo().substring(0, 8));
        OrderPageResponse recentPage = fetchOrderPage(
                "/api/orders?userId={userId}&fromDate={fromDate}&toDate={toDate}",
                userId,
                LocalDate.now().minusDays(1),
                LocalDate.now()
        );
        ResponseEntity<String> invalidStatus = restTemplate.getForEntity("/api/orders?status=UNKNOWN", String.class);

        assertThat(defaultPage.getPage()).isEqualTo(0);
        assertThat(defaultPage.getSize()).isEqualTo(10);
        assertThat(defaultPage.getTotalElements()).isEqualTo(4);
        assertThat(defaultPage.getTotalPages()).isEqualTo(1);
        assertThat(defaultPage.isFirst()).isTrue();
        assertThat(defaultPage.isLast()).isTrue();

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.isFirst()).isTrue();
        assertThat(firstPage.isLast()).isFalse();
        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.isLast()).isTrue();

        assertThat(pendingPage.getContent()).extracting(OrderResponse::getStatus).containsOnly("PENDING_PAYMENT");
        assertThat(paidPage.getContent()).extracting(OrderResponse::getStatus).containsOnly("PAID");
        assertThat(closedPage.getContent()).extracting(OrderResponse::getStatus).containsOnly("CLOSED");
        assertThat(refundedPage.getContent()).extracting(OrderResponse::getStatus).containsOnly("REFUNDED");
        assertThat(pendingPage.getContent()).extracting(OrderResponse::getId).contains(pending.getId());
        assertThat(paidPage.getContent()).extracting(OrderResponse::getId).contains(paid.getId());
        assertThat(closedPage.getContent()).extracting(OrderResponse::getId).contains(closed.getId());
        assertThat(refundedPage.getContent()).extracting(OrderResponse::getId).contains(refunded.getId());

        assertThat(defaultPage.getContent()).extracting(OrderResponse::getUserId).containsOnly(userId);
        assertThat(orderNoPage.getContent()).extracting(OrderResponse::getOrderNo).contains(paid.getOrderNo());
        assertThat(recentPage.getContent()).extracting(OrderResponse::getId)
                .contains(paid.getId(), closed.getId(), refunded.getId())
                .doesNotContain(pending.getId());
        assertThat(invalidStatus.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldReturnExistingOrderForDuplicateRequestId() {
        TrainSearchResponse before = firstTrainInventory();
        long userId = 3107L;
        String requestId = "idem-" + System.nanoTime();
        long orderCountBefore = ticketOrderRepository.countByInventory_Id(before.getInventoryId());

        OrderResponse first = createOrder(userId, before, "IdempotentUserA", requestId);
        OrderResponse second = createOrder(userId, before, "IdempotentUserB", requestId);
        TrainSearchResponse after = findInventory(before.getInventoryId());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getOrderNo()).isEqualTo(first.getOrderNo());
        assertThat(first.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(second.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(second.getRequestId()).isEqualTo(requestId);
        assertThat(after.getRemainingSeats()).isEqualTo(before.getRemainingSeats() - 1);
        assertThat(ticketOrderRepository.countByInventory_Id(before.getInventoryId())).isEqualTo(orderCountBefore + 1);
    }

    @Test
    void shouldRefundOrderAndReleaseInventory() {
        TrainSearchResponse before = firstTrainInventory();
        long userId = 3102L;

        OrderResponse created = createPaidOrder(userId, before, "RefundUser");
        TrainSearchResponse afterCreate = findInventory(before.getInventoryId());

        OrderResponse refunded = restTemplate.postForObject(
                "/api/orders/{id}/refund",
                null,
                OrderResponse.class,
                created.getId()
        );
        TrainSearchResponse afterRefund = findInventory(before.getInventoryId());

        assertThat(afterCreate.getRemainingSeats()).isEqualTo(before.getRemainingSeats() - 1);
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(afterRefund.getRemainingSeats()).isEqualTo(before.getRemainingSeats());
    }

    @Test
    void shouldManagePassengerTravelersAndUseTravelerSnapshotForOrderAndTicket() {
        AuthResponse passenger = login("passenger1", "123456");
        AuthResponse otherPassenger = login("passenger2", "123456");
        String suffix = String.valueOf(Math.abs(System.nanoTime()));
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));

        PassengerTravelerRequest request = new PassengerTravelerRequest();
        request.setPassengerName("TravelerUser" + suffix);
        request.setIdType("ID_CARD");
        request.setIdNo("33010119990101" + suffix.substring(0, 4));
        request.setPhone("136" + suffix.substring(0, 8));
        request.setDefaultTraveler(true);

        ResponseEntity<PassengerTravelerResponse> createdResponse = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.POST,
                authorizedEntity(passenger, request),
                PassengerTravelerResponse.class
        );
        assertThat(createdResponse.getStatusCodeValue()).isEqualTo(200);
        PassengerTravelerResponse traveler = createdResponse.getBody();
        assertThat(traveler).isNotNull();
        assertThat(traveler.getPassengerName()).isEqualTo(request.getPassengerName());
        assertThat(traveler.getIdNoMasked()).contains("*");
        assertThat(traveler.getPhoneMasked()).contains("*");

        ResponseEntity<PassengerTravelerResponse[]> listResponse = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.GET,
                authorizedEntity(passenger),
                PassengerTravelerResponse[].class
        );
        assertThat(listResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(Arrays.asList(listResponse.getBody())).extracting(PassengerTravelerResponse::getId)
                .contains(traveler.getId());

        ResponseEntity<PassengerTravelerResponse[]> otherListResponse = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.GET,
                authorizedEntity(otherPassenger),
                PassengerTravelerResponse[].class
        );
        assertThat(Arrays.asList(otherListResponse.getBody())).extracting(PassengerTravelerResponse::getId)
                .doesNotContain(traveler.getId());

        ResponseEntity<PassengerTravelerResponse> defaultResponse = restTemplate.exchange(
                "/api/passenger/travelers/{id}/default",
                HttpMethod.POST,
                authorizedEntity(passenger),
                PassengerTravelerResponse.class,
                traveler.getId()
        );
        assertThat(defaultResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(defaultResponse.getBody()).isNotNull();
        assertThat(defaultResponse.getBody().isDefaultTraveler()).isTrue();

        PassengerTravelerRequest secondRequest = new PassengerTravelerRequest();
        secondRequest.setPassengerName("TravelerDefault" + suffix);
        secondRequest.setIdType("PASSPORT");
        secondRequest.setIdNo("P" + suffix + "X");
        secondRequest.setPhone("137" + suffix.substring(0, 8));
        secondRequest.setDefaultTraveler(true);
        PassengerTravelerResponse secondTraveler = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.POST,
                authorizedEntity(passenger, secondRequest),
                PassengerTravelerResponse.class
        ).getBody();
        assertThat(secondTraveler).isNotNull();
        assertThat(secondTraveler.isDefaultTraveler()).isTrue();

        ResponseEntity<PassengerTravelerResponse[]> defaultListResponse = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.GET,
                authorizedEntity(passenger),
                PassengerTravelerResponse[].class
        );
        assertThat(Arrays.asList(defaultListResponse.getBody()).stream()
                .filter(PassengerTravelerResponse::isDefaultTraveler)
                .count()).isEqualTo(1);

        PassengerTravelerRequest updateRequest = new PassengerTravelerRequest();
        updateRequest.setPassengerName(request.getPassengerName() + "Updated");
        updateRequest.setIdType("ID_CARD");
        updateRequest.setDefaultTraveler(false);
        PassengerTravelerResponse updatedTraveler = restTemplate.exchange(
                "/api/passenger/travelers/{id}",
                HttpMethod.PUT,
                authorizedEntity(passenger, updateRequest),
                PassengerTravelerResponse.class,
                traveler.getId()
        ).getBody();
        assertThat(updatedTraveler).isNotNull();
        assertThat(updatedTraveler.getPassengerName()).isEqualTo(updateRequest.getPassengerName());
        assertThat(updatedTraveler.getIdNoMasked()).isEqualTo(traveler.getIdNoMasked());
        assertThat(updatedTraveler.getPhoneMasked()).isEqualTo(traveler.getPhoneMasked());
        traveler = updatedTraveler;

        PassengerTravelerRequest deleteRequest = new PassengerTravelerRequest();
        deleteRequest.setPassengerName("TravelerDelete" + suffix);
        deleteRequest.setIdType("OTHER");
        deleteRequest.setIdNo("DEL" + suffix);
        deleteRequest.setDefaultTraveler(false);
        PassengerTravelerResponse deleteTarget = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.POST,
                authorizedEntity(passenger, deleteRequest),
                PassengerTravelerResponse.class
        ).getBody();
        assertThat(deleteTarget).isNotNull();
        restTemplate.exchange(
                "/api/passenger/travelers/{id}",
                HttpMethod.DELETE,
                authorizedEntity(passenger),
                Void.class,
                deleteTarget.getId()
        );
        ResponseEntity<PassengerTravelerResponse[]> afterDeleteResponse = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.GET,
                authorizedEntity(passenger),
                PassengerTravelerResponse[].class
        );
        assertThat(Arrays.asList(afterDeleteResponse.getBody())).extracting(PassengerTravelerResponse::getId)
                .doesNotContain(deleteTarget.getId());

        PassengerTravelerRequest illegalUpdate = new PassengerTravelerRequest();
        illegalUpdate.setPassengerName("OtherUpdate");
        illegalUpdate.setIdType("ID_CARD");
        illegalUpdate.setIdNo("110101199001019999");
        ResponseEntity<String> otherUpdate = restTemplate.exchange(
                "/api/passenger/travelers/{id}",
                HttpMethod.PUT,
                authorizedEntity(otherPassenger, illegalUpdate),
                String.class,
                traveler.getId()
        );
        assertThat(otherUpdate.getStatusCodeValue()).isEqualTo(403);

        TrainSearchResponse train = firstTrainInventory();
        PassengerCreateOrderRequest orderRequest = new PassengerCreateOrderRequest();
        orderRequest.setTrainId(train.getTrainId());
        orderRequest.setInventoryId(train.getInventoryId());
        orderRequest.setTravelerId(traveler.getId());
        orderRequest.setRequestId("traveler-order-" + suffix);

        ResponseEntity<String> otherCreate = restTemplate.exchange(
                "/api/passenger/orders",
                HttpMethod.POST,
                authorizedEntity(otherPassenger, orderRequest),
                String.class
        );
        assertThat(otherCreate.getStatusCodeValue()).isEqualTo(403);

        ResponseEntity<OrderResponse> orderResponse = restTemplate.exchange(
                "/api/passenger/orders",
                HttpMethod.POST,
                authorizedEntity(passenger, orderRequest),
                OrderResponse.class
        );
        assertThat(orderResponse.getStatusCodeValue()).isEqualTo(200);
        OrderResponse order = orderResponse.getBody();
        assertThat(order).isNotNull();
        assertThat(order.getPassengerName()).isEqualTo(traveler.getPassengerName());
        assertThat(order.getPassengerIdType()).isEqualTo("ID_CARD");
        assertThat(order.getPassengerIdNoMasked()).isEqualTo(traveler.getIdNoMasked());
        assertThat(order.getPassengerPhoneMasked()).isEqualTo(traveler.getPhoneMasked());

        restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger),
                OrderResponse.class,
                order.getId()
        );

        ResponseEntity<OrderDetailResponse> detail = restTemplate.exchange(
                "/api/passenger/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(passenger),
                OrderDetailResponse.class,
                order.getId()
        );
        assertThat(detail.getStatusCodeValue()).isEqualTo(200);
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().getOrder().getPassengerIdNoMasked()).isEqualTo(traveler.getIdNoMasked());
        assertThat(detail.getBody().getTicket()).isNotNull();
        assertThat(detail.getBody().getTicket().getPassengerIdType()).isEqualTo("ID_CARD");
        assertThat(detail.getBody().getTicket().getPassengerIdCardMasked()).isEqualTo(traveler.getIdNoMasked());
        assertThat(detail.getBody().getTicket().getPassengerPhoneMasked()).isEqualTo(traveler.getPhoneMasked());
    }

    @Test
    void shouldIssueTicketAndReturnPassengerOrderDetail() {
        AuthResponse passenger = login("passenger1", "123456");
        AuthResponse otherPassenger = login("passenger2", "123456");
        TrainSearchResponse train = firstTrainInventory();

        OrderResponse pending = createPassengerOrder(passenger, train, "PassengerTicketUser");
        ResponseEntity<OrderDetailResponse> pendingDetail = restTemplate.exchange(
                "/api/passenger/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(passenger),
                OrderDetailResponse.class,
                pending.getId()
        );
        assertThat(pendingDetail.getStatusCodeValue()).isEqualTo(200);
        assertThat(pendingDetail.getBody()).isNotNull();
        assertThat(pendingDetail.getBody().getTicket()).isNull();

        ResponseEntity<OrderResponse> paidResponse = restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger),
                OrderResponse.class,
                pending.getId()
        );
        assertThat(paidResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(paidResponse.getBody()).isNotNull();
        assertThat(paidResponse.getBody().getStatus()).isEqualTo("PAID");

        TicketRecord ticket = ticketRecordRepository.findByOrderId(pending.getId())
                .orElseThrow(() -> new AssertionError("expected ticket"));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        assertThat(ticket.getPassengerIdCardMasked()).contains("****");

        ResponseEntity<OrderDetailResponse> detail = restTemplate.exchange(
                "/api/passenger/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(passenger),
                OrderDetailResponse.class,
                pending.getId()
        );
        assertThat(detail.getStatusCodeValue()).isEqualTo(200);
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().getTicket()).isNotNull();
        assertThat(detail.getBody().getTicket().getStatus()).isEqualTo("ISSUED");
        assertThat(detail.getBody().getPayments()).isNotEmpty();
        assertThat(detail.getBody().getRisks()).isEmpty();
        assertThat(detail.getBody().getOutboxEvents()).isEmpty();

        TicketPageResponse ticketPage = restTemplate.exchange(
                "/api/passenger/tickets?status=ISSUED&size=50",
                HttpMethod.GET,
                authorizedEntity(passenger),
                TicketPageResponse.class
        ).getBody();
        TicketPageResponse otherTicketPage = restTemplate.exchange(
                "/api/passenger/tickets?size=50",
                HttpMethod.GET,
                authorizedEntity(otherPassenger),
                TicketPageResponse.class
        ).getBody();
        assertThat(ticketPage).isNotNull();
        assertThat(ticketPage.getContent()).extracting(ticketResponse -> ticketResponse.getTicketNo())
                .contains(ticket.getTicketNo());
        assertThat(ticketPage.getContent()).allSatisfy(ticketResponse -> assertThat(ticketResponse.getUserId()).isEqualTo(pending.getUserId()));
        assertThat(otherTicketPage).isNotNull();
        assertThat(otherTicketPage.getContent()).extracting(ticketResponse -> ticketResponse.getTicketNo())
                .doesNotContain(ticket.getTicketNo());

        ResponseEntity<String> otherDetail = restTemplate.exchange(
                "/api/passenger/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(otherPassenger),
                String.class,
                pending.getId()
        );
        assertThat(otherDetail.getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    void shouldChangePassengerTicketAndExposeTrackingToAdmin() {
        AuthResponse passenger = login("passenger1", "123456");
        AuthResponse otherPassenger = login("passenger2", "123456");
        AuthResponse admin = login("admin", "admin123");
        TrainSearchResponse oldTrain = createTicketChangeInventory(new BigDecimal("80.00"), "OLD");
        TrainSearchResponse newTrain = createTicketChangeInventory(new BigDecimal("120.00"), "NEW");

        OrderResponse pending = createPassengerOrder(passenger, oldTrain, "PassengerChangeUser");
        ResponseEntity<OrderResponse> paidResponse = restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger),
                OrderResponse.class,
                pending.getId()
        );
        assertThat(paidResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(paidResponse.getBody()).isNotNull();
        assertThat(paidResponse.getBody().getStatus()).isEqualTo("PAID");

        PassengerChangeTicketRequest request = new PassengerChangeTicketRequest();
        request.setTrainId(newTrain.getTrainId());
        request.setInventoryId(newTrain.getInventoryId());
        request.setRequestId("ticket-change-" + System.nanoTime());
        request.setReason("schedule changed");

        ResponseEntity<TicketChangeResponse> createdResponse = restTemplate.exchange(
                "/api/passenger/orders/{id}/change",
                HttpMethod.POST,
                authorizedEntity(passenger, request),
                TicketChangeResponse.class,
                pending.getId()
        );
        assertThat(createdResponse.getStatusCodeValue()).isEqualTo(200);
        TicketChangeResponse created = createdResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.getOriginalOrderId()).isEqualTo(pending.getId());
        assertThat(created.getNewOrderId()).isNotNull();
        assertThat(created.getPriceDifference()).isEqualByComparingTo("40.00");
        assertThat(ticketOrderRepository.findById(pending.getId()).orElseThrow(() -> new AssertionError("expected order")).getStatus())
                .isEqualTo(OrderStatus.PAID);

        ResponseEntity<String> otherChangeResponse = restTemplate.exchange(
                "/api/passenger/orders/{id}/change",
                HttpMethod.POST,
                authorizedEntity(otherPassenger, request),
                String.class,
                pending.getId()
        );
        assertThat(otherChangeResponse.getStatusCodeValue()).isEqualTo(403);

        ResponseEntity<TicketChangePageResponse> passengerChanges = restTemplate.exchange(
                "/api/passenger/changes?status=PENDING_PAYMENT&size=50",
                HttpMethod.GET,
                authorizedEntity(passenger),
                TicketChangePageResponse.class
        );
        assertThat(passengerChanges.getStatusCodeValue()).isEqualTo(200);
        assertThat(passengerChanges.getBody()).isNotNull();
        assertThat(passengerChanges.getBody().getContent()).extracting(TicketChangeResponse::getChangeNo)
                .contains(created.getChangeNo());

        ResponseEntity<TicketChangePageResponse> adminChanges = restTemplate.exchange(
                "/api/ticket-changes?changeNo={changeNo}",
                HttpMethod.GET,
                authorizedEntity(admin),
                TicketChangePageResponse.class,
                created.getChangeNo()
        );
        assertThat(adminChanges.getStatusCodeValue()).isEqualTo(200);
        assertThat(adminChanges.getBody()).isNotNull();
        assertThat(adminChanges.getBody().getContent()).extracting(TicketChangeResponse::getChangeNo)
                .contains(created.getChangeNo());

        ResponseEntity<String> userAdminAccess = restTemplate.exchange(
                "/api/ticket-changes",
                HttpMethod.GET,
                authorizedEntity(passenger),
                String.class
        );
        assertThat(userAdminAccess.getStatusCodeValue()).isEqualTo(403);

        ResponseEntity<PassengerTransactionSummaryResponse> summaryResponse = restTemplate.exchange(
                "/api/passenger/transactions/summary",
                HttpMethod.GET,
                authorizedEntity(passenger),
                PassengerTransactionSummaryResponse.class
        );
        assertThat(summaryResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(summaryResponse.getBody()).isNotNull();
        assertThat(summaryResponse.getBody().getPendingChangeCount()).isGreaterThanOrEqualTo(1);
        assertThat(summaryResponse.getBody().getLatestChanges()).extracting(TicketChangeResponse::getChangeNo)
                .contains(created.getChangeNo());
        assertThat(summaryResponse.getBody().getTodoItems()).extracting(PassengerTodoItemResponse::getType)
                .contains("CHANGE_PAYMENT");

        ResponseEntity<TicketChangeResponse> paidChangeResponse = restTemplate.exchange(
                "/api/passenger/changes/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger),
                TicketChangeResponse.class,
                created.getId()
        );
        assertThat(paidChangeResponse.getStatusCodeValue()).isEqualTo(200);
        TicketChangeResponse success = paidChangeResponse.getBody();
        assertThat(success).isNotNull();
        assertThat(success.getStatus()).isEqualTo("SUCCESS");
        assertThat(success.getNewTicketNo()).isNotBlank();

        TicketOrder originalOrder = ticketOrderRepository.findById(pending.getId())
                .orElseThrow(() -> new AssertionError("expected original order"));
        TicketOrder newOrder = ticketOrderRepository.findById(success.getNewOrderId())
                .orElseThrow(() -> new AssertionError("expected new order"));
        TicketRecord originalTicket = ticketRecordRepository.findByOrderId(originalOrder.getId())
                .orElseThrow(() -> new AssertionError("expected original ticket"));
        TicketRecord newTicket = ticketRecordRepository.findByOrderId(newOrder.getId())
                .orElseThrow(() -> new AssertionError("expected new ticket"));

        assertThat(originalOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(originalTicket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(newTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);
        assertThat(refundRecordRepository.findByOrderIdOrderByCreatedAtDesc(originalOrder.getId())).isNotEmpty();
        assertThat(notificationRecordRepository.findAll()).extracting(NotificationRecord::getType)
                .contains(NotificationType.TICKET_CHANGE_CREATED, NotificationType.TICKET_CHANGE_SUCCEEDED);
        assertThat(outboxEventRepository.findAll()).extracting(OutboxEvent::getEventType)
                .contains(OutboxEventTypes.TICKET_CHANGE_CREATED, OutboxEventTypes.TICKET_CHANGE_SUCCEEDED);

        ResponseEntity<OrderDetailResponse> originalDetail = restTemplate.exchange(
                "/api/passenger/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(passenger),
                OrderDetailResponse.class,
                originalOrder.getId()
        );
        ResponseEntity<OrderDetailResponse> newDetail = restTemplate.exchange(
                "/api/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(admin),
                OrderDetailResponse.class,
                newOrder.getId()
        );
        assertThat(originalDetail.getStatusCodeValue()).isEqualTo(200);
        assertThat(newDetail.getStatusCodeValue()).isEqualTo(200);
        assertThat(originalDetail.getBody()).isNotNull();
        assertThat(newDetail.getBody()).isNotNull();
        assertThat(originalDetail.getBody().getTicketChanges()).extracting(TicketChangeResponse::getChangeNo)
                .contains(created.getChangeNo());
        assertThat(newDetail.getBody().getTicketChanges()).extracting(TicketChangeResponse::getChangeNo)
                .contains(created.getChangeNo());
        assertThat(originalDetail.getBody().getNotifications()).extracting(NotificationResponse::getType)
                .contains(NotificationType.TICKET_CHANGE_CREATED.name());
        assertThat(newDetail.getBody().getNotifications()).extracting(NotificationResponse::getType)
                .contains(NotificationType.TICKET_CHANGE_PENDING_PAYMENT.name(), NotificationType.TICKET_CHANGE_SUCCEEDED.name());

        AdminGlobalSearchResponse changeSearch = search(admin,
                "/api/search?keyword={keyword}&types=TICKET_CHANGE&limitPerType=3&includeTrace=true",
                created.getChangeNo());
        SearchResultGroupResponse changeGroup = group(changeSearch, "TICKET_CHANGE");
        assertThat(changeGroup.getItems()).extracting(SearchResultItemResponse::getChangeNo).contains(created.getChangeNo());
        assertThat(changeGroup.getItems().get(0).getTrace()).isNotEmpty();
    }

    @Test
    void shouldInvalidateTicketAfterRefundAndExposeAdminOrderDetail() {
        AuthResponse admin = login("admin", "admin123");
        TrainSearchResponse train = firstTrainInventory();

        OrderResponse paid = createPaidOrder(7102L, train, "AdminTicketDetailUser");
        TicketRecord issuedTicket = ticketRecordRepository.findByOrderId(paid.getId())
                .orElseThrow(() -> new AssertionError("expected issued ticket"));
        assertThat(issuedTicket.getStatus()).isEqualTo(TicketStatus.ISSUED);

        OrderResponse refunded = refundOrder(paid.getId());
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");

        TicketRecord refundedTicket = ticketRecordRepository.findByOrderId(paid.getId())
                .orElseThrow(() -> new AssertionError("expected refunded ticket"));
        assertThat(refundedTicket.getStatus()).isEqualTo(TicketStatus.REFUNDED);
        assertThat(refundedTicket.getInvalidatedAt()).isNotNull();

        ResponseEntity<OrderDetailResponse> detail = restTemplate.exchange(
                "/api/orders/{id}/detail",
                HttpMethod.GET,
                authorizedEntity(admin),
                OrderDetailResponse.class,
                paid.getId()
        );
        assertThat(detail.getStatusCodeValue()).isEqualTo(200);
        assertThat(detail.getBody()).isNotNull();
        assertThat(detail.getBody().getOrder().getStatus()).isEqualTo("REFUNDED");
        assertThat(detail.getBody().getTicket()).isNotNull();
        assertThat(detail.getBody().getTicket().getStatus()).isEqualTo("REFUNDED");
        assertThat(detail.getBody().getRefunds()).isNotEmpty();
        assertThat(detail.getBody().getOutboxEvents())
                .extracting(OutboxEventResponse::getEventType)
                .contains(OutboxEventTypes.ORDER_REFUNDED, OutboxEventTypes.TICKET_REFUNDED);
    }

    @Test
    void shouldRejectInvalidOrderStateTransitions() {
        TrainSearchResponse closeBefore = firstTrainInventory();
        OrderResponse pendingToClose = createOrder(3109L, closeBefore, "ClosePendingUser");
        OrderResponse closed = closeOrder(pendingToClose.getId());
        TrainSearchResponse afterClose = findInventory(closeBefore.getInventoryId());

        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getClosedAt()).isNotNull();
        assertThat(afterClose.getRemainingSeats()).isEqualTo(closeBefore.getRemainingSeats());
        assertThat(payOrderRaw(closed.getId()).getStatusCodeValue()).isEqualTo(400);
        assertThat(closeOrderRaw(closed.getId()).getStatusCodeValue()).isEqualTo(400);

        TrainSearchResponse paidBefore = firstTrainInventory();
        OrderResponse paid = createPaidOrder(3110L, paidBefore, "PaidCannotCloseUser");
        assertThat(closeOrderRaw(paid.getId()).getStatusCodeValue()).isEqualTo(400);

        TrainSearchResponse refundBefore = firstTrainInventory();
        OrderResponse pendingToRefund = createOrder(3111L, refundBefore, "PendingCannotRefundUser");
        assertThat(refundOrderRaw(pendingToRefund.getId()).getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldKeepPaymentIdempotentWithoutDuplicateRiskEvents() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3112L;

        createPaidOrder(userId, train, "PaymentRiskA");
        createPaidOrder(userId, train, "PaymentRiskB");
        OrderResponse thirdPaid = createPaidOrder(userId, train, "PaymentRiskC");
        long riskCountAfterPay = countRisksForUser(userId);

        OrderResponse duplicatePay = payOrder(thirdPaid.getId());
        long riskCountAfterDuplicatePay = countRisksForUser(userId);

        assertThat(thirdPaid.getStatus()).isEqualTo("PAID");
        assertThat(duplicatePay.getStatus()).isEqualTo("PAID");
        assertThat(duplicatePay.getId()).isEqualTo(thirdPaid.getId());
        assertThat(riskCountAfterPay).isGreaterThan(0);
        assertThat(riskCountAfterDuplicatePay).isEqualTo(riskCountAfterPay);
    }

    @Test
    void shouldRejectDuplicateRefund() {
        TrainSearchResponse before = firstTrainInventory();
        OrderResponse paid = createPaidOrder(3113L, before, "DuplicateRefundUser");

        OrderResponse refunded = refundOrder(paid.getId());
        ResponseEntity<String> duplicateRefund = refundOrderRaw(paid.getId());

        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(duplicateRefund.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldCreatePaymentAndHandleSuccessCallbackIdempotently() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3130L;
        createPaidOrder(userId, train, "PaymentCallbackRiskA");
        createPaidOrder(userId, train, "PaymentCallbackRiskB");
        OrderResponse pending = createOrder(userId, train, "PaymentCallbackRiskC");
        long riskCountBeforeCallback = countRisksForUser(userId);
        String requestId = "pay-create-" + System.nanoTime();

        PaymentResponse created = createPayment(pending.getId(), requestId);
        PaymentResponse duplicateCreate = createPayment(pending.getId(), requestId);
        PaymentResponse success = callbackPayment(created.getPaymentNo(), "callback-success-" + System.nanoTime(), true);
        long riskCountAfterCallback = countRisksForUser(userId);
        PaymentResponse duplicateCallback = callbackPayment(created.getPaymentNo(), success.getCallbackRequestId(), true);
        long riskCountAfterDuplicateCallback = countRisksForUser(userId);
        TicketOrder paidOrder = ticketOrderRepository.findById(pending.getId())
                .orElseThrow(() -> new AssertionError("expected order"));

        assertThat(created.getStatus()).isEqualTo("PENDING");
        assertThat(duplicateCreate.getId()).isEqualTo(created.getId());
        assertThat(success.getStatus()).isEqualTo("SUCCESS");
        assertThat(success.getChannelPaymentNo()).isNotBlank();
        assertThat(success.getPaidAt()).isNotNull();
        assertThat(paidOrder.getStatus().name()).isEqualTo("PAID");
        assertThat(riskCountAfterCallback).isGreaterThan(riskCountBeforeCallback);
        assertThat(duplicateCallback.getId()).isEqualTo(success.getId());
        assertThat(riskCountAfterDuplicateCallback).isEqualTo(riskCountAfterCallback);
        assertThat(ticketOrderRepository.findById(pending.getId())
                .orElseThrow(() -> new AssertionError("expected order"))
                .getStatus().name()).isEqualTo("PAID");
    }

    @Test
    void shouldRejectPaymentCallbackWithInvalidSignatureOrAmount() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3138L;
        OrderResponse pending = createOrder(userId, train, "PaymentVerifyUser");
        PaymentResponse payment = createPayment(pending.getId(), "pay-verify-" + System.nanoTime());
        long riskCountBefore = countRisksForUser(userId);

        PaymentCallbackRequest badSignature = signedPaymentCallbackRequest(
                payment.getPaymentNo(),
                "callback-bad-signature-" + System.nanoTime(),
                true
        );
        badSignature.setSignature("bad-signature");
        ResponseEntity<String> badSignatureResponse = restTemplate.postForEntity(
                "/api/payments/callback",
                badSignature,
                String.class
        );

        PaymentCallbackRequest missingSignature = signedPaymentCallbackRequest(
                payment.getPaymentNo(),
                "callback-missing-signature-" + System.nanoTime(),
                true
        );
        missingSignature.setSignature(null);
        ResponseEntity<String> missingSignatureResponse = restTemplate.postForEntity(
                "/api/payments/callback",
                missingSignature,
                String.class
        );

        PaymentCallbackRequest wrongAmount = signedPaymentCallbackRequest(
                payment.getPaymentNo(),
                "callback-wrong-amount-" + System.nanoTime(),
                true
        );
        wrongAmount.setAmount(wrongAmount.getAmount().add(BigDecimal.ONE));
        wrongAmount.setSignature(signPaymentCallback(wrongAmount));
        ResponseEntity<String> wrongAmountResponse = restTemplate.postForEntity(
                "/api/payments/callback",
                wrongAmount,
                String.class
        );

        TicketOrder afterRejectedCallbacks = ticketOrderRepository.findById(pending.getId())
                .orElseThrow(() -> new AssertionError("expected order"));

        assertThat(badSignatureResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(missingSignatureResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(wrongAmountResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(afterRejectedCallbacks.getStatus().name()).isEqualTo("PENDING_PAYMENT");
        assertThat(countRisksForUser(userId)).isEqualTo(riskCountBefore);
    }

    @Test
    void shouldCreateRefundRecordAndHandleRefundCallbacksIdempotently() {
        TrainSearchResponse train = firstTrainInventory();
        OrderResponse paid = createPaidOrderThroughPayment(3140L, train, "RefundFlowUser");

        OrderResponse refunded = refundOrder(paid.getId());
        RefundPageResponse refundPage = fetchRefundPage("/api/refunds?orderId={orderId}", paid.getId());
        RefundResponse refund = refundPage.getContent().get(0);
        RefundPageResponse refundNoPage = fetchRefundPage("/api/refunds?refundNo={refundNo}", refund.getRefundNo());
        RefundPageResponse pendingPage = fetchRefundPage("/api/refunds?status=PENDING");

        RefundResponse success = callbackRefund(refund.getRefundNo(), "refund-callback-success-" + System.nanoTime(), true);
        RefundResponse duplicate = callbackRefund(refund.getRefundNo(), success.getCallbackRequestId(), true);
        List<OperationLog> refundSuccessLogs = Arrays.asList(latestLogs(login("admin", "admin123")));

        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(refund.getStatus()).isEqualTo("PENDING");
        assertThat(refund.getPaymentNo()).isNotBlank();
        assertThat(refundNoPage.getContent()).extracting(RefundResponse::getRefundNo).contains(refund.getRefundNo());
        assertThat(pendingPage.getContent()).extracting(RefundResponse::getRefundNo).contains(refund.getRefundNo());
        assertThat(success.getStatus()).isEqualTo("SUCCESS");
        assertThat(success.getChannelRefundNo()).isNotBlank();
        assertThat(success.getRefundedAt()).isNotNull();
        assertThat(duplicate.getId()).isEqualTo(success.getId());
        assertThat(refundSuccessLogs).filteredOn(log -> "REFUND_SUCCESS".equals(log.getAction())
                && success.getRefundNo().equals(log.getTargetId())).hasSize(1);
    }

    @Test
    void shouldRejectInvalidRefundCallbacksAndKeepFailedRefundFinal() {
        TrainSearchResponse train = firstTrainInventory();
        OrderResponse paidForInvalid = createPaidOrderThroughPayment(3141L, train, "RefundInvalidUser");
        refundOrder(paidForInvalid.getId());
        RefundResponse pendingRefund = fetchRefundPage("/api/refunds?orderId={orderId}", paidForInvalid.getId()).getContent().get(0);

        RefundCallbackRequest badSignature = signedRefundCallbackRequest(
                pendingRefund.getRefundNo(),
                "refund-callback-bad-signature-" + System.nanoTime(),
                true
        );
        badSignature.setSignature("bad-signature");
        ResponseEntity<String> badSignatureResponse = restTemplate.postForEntity(
                "/api/refunds/callback",
                badSignature,
                String.class
        );

        RefundCallbackRequest wrongAmount = signedRefundCallbackRequest(
                pendingRefund.getRefundNo(),
                "refund-callback-wrong-amount-" + System.nanoTime(),
                true
        );
        wrongAmount.setAmount(wrongAmount.getAmount().add(BigDecimal.ONE));
        wrongAmount.setSignature(signRefundCallback(wrongAmount));
        ResponseEntity<String> wrongAmountResponse = restTemplate.postForEntity(
                "/api/refunds/callback",
                wrongAmount,
                String.class
        );
        RefundRecord afterRejected = refundRecordRepository.findByRefundNo(pendingRefund.getRefundNo())
                .orElseThrow(() -> new AssertionError("expected refund"));

        OrderResponse paidForFailed = createPaidOrderThroughPayment(3142L, train, "RefundFailedUser");
        refundOrder(paidForFailed.getId());
        RefundResponse failedRefund = fetchRefundPage("/api/refunds?orderId={orderId}", paidForFailed.getId()).getContent().get(0);
        RefundResponse failed = callbackRefund(failedRefund.getRefundNo(), "refund-callback-failed-" + System.nanoTime(), false);
        ResponseEntity<String> successAfterFailed = callbackRefundRaw(
                failedRefund.getRefundNo(),
                "refund-callback-success-after-failed-" + System.nanoTime(),
                true
        );

        assertThat(badSignatureResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(wrongAmountResponse.getStatusCodeValue()).isEqualTo(400);
        assertThat(afterRejected.getStatus().name()).isEqualTo("PENDING");
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(successAfterFailed.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldPublishAndDispatchOutboxEventsForTransactions() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3190L;
        long paymentSucceededBefore = countOutboxEvents(OutboxEventTypes.PAYMENT_SUCCEEDED);
        long orderPaidBefore = countOutboxEvents(OutboxEventTypes.ORDER_PAID);
        long refundCreatedBefore = countOutboxEvents(OutboxEventTypes.REFUND_CREATED);
        long orderRefundedBefore = countOutboxEvents(OutboxEventTypes.ORDER_REFUNDED);
        long refundSucceededBefore = countOutboxEvents(OutboxEventTypes.REFUND_SUCCEEDED);
        long riskHandledBefore = countOutboxEvents(OutboxEventTypes.RISK_EVENT_HANDLED);

        OrderResponse paid = createPaidOrderThroughPayment(userId, train, "OutboxPaidUser");
        OrderResponse refunded = refundOrder(paid.getId());
        RefundResponse refund = fetchRefundPage("/api/refunds?orderId={orderId}", refunded.getId()).getContent().get(0);
        callbackRefund(refund.getRefundNo(), "outbox-refund-success-" + System.nanoTime(), true);

        RiskEventResponse risk = createPendingRisk(3191L, train, "OutboxRiskUser");
        AuthResponse admin = login("admin", "admin123");
        handleRisk(risk.getId(), admin, "CONFIRMED", "outbox handle event");

        assertThat(countOutboxEvents(OutboxEventTypes.PAYMENT_SUCCEEDED)).isGreaterThan(paymentSucceededBefore);
        assertThat(countOutboxEvents(OutboxEventTypes.ORDER_PAID)).isGreaterThan(orderPaidBefore);
        assertThat(countOutboxEvents(OutboxEventTypes.REFUND_CREATED)).isGreaterThan(refundCreatedBefore);
        assertThat(countOutboxEvents(OutboxEventTypes.ORDER_REFUNDED)).isGreaterThan(orderRefundedBefore);
        assertThat(countOutboxEvents(OutboxEventTypes.REFUND_SUCCEEDED)).isGreaterThan(refundSucceededBefore);
        assertThat(countOutboxEvents(OutboxEventTypes.RISK_EVENT_HANDLED)).isGreaterThan(riskHandledBefore);

        OutboxEventPageResponse paymentEvents = fetchOutboxPage(
                admin,
                "/api/outbox-events?status=PENDING&eventType=PAYMENT_SUCCEEDED&page=0&size=10"
        );
        assertThat(paymentEvents.getContent()).isNotEmpty();
        String eventId = paymentEvents.getContent().get(0).getEventId();

        ResponseEntity<OutboxDispatchResponse> dispatchResponse = restTemplate.exchange(
                "/api/outbox-events/dispatch",
                HttpMethod.POST,
                authorizedEntity(admin),
                OutboxDispatchResponse.class
        );
        assertThat(dispatchResponse.getBody()).isNotNull();
        assertThat(dispatchResponse.getBody().getProcessedCount()).isGreaterThan(0);

        OutboxEvent dispatched = outboxEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new AssertionError("expected outbox event"));
        assertThat(dispatched.getStatus()).isEqualTo(OutboxEventStatus.DONE);
        assertThat(dispatched.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldRetryAndFailOutboxEventWhenHandlerIsMissing() {
        OutboxEvent event = outboxEventPublisher.publish(
                "UNSUPPORTED_TEST_EVENT",
                "TEST",
                "missing-handler",
                "{}",
                1
        );

        int processedCount = 0;
        OutboxEvent failed = event;
        for (int i = 0; i < 10; i++) {
            processedCount += outboxEventDispatcher.dispatchOnce(1);
            failed = outboxEventRepository.findByEventId(event.getEventId())
                    .orElseThrow(() -> new AssertionError("expected outbox event"));
            if (failed.getStatus() != OutboxEventStatus.PENDING) {
                break;
            }
        }

        assertThat(processedCount).isGreaterThanOrEqualTo(1);
        assertThat(failed.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastError()).contains("No outbox handler");
    }

    @Test
    void shouldRequeueFailedOutboxEventManually() {
        AuthResponse admin = login("admin", "admin123");
        OutboxEvent event = outboxEventPublisher.publish(
                "UNSUPPORTED_RETRY_EVENT",
                "TEST",
                "retry-one",
                "{}",
                1
        );
        markOutboxEventFailed(event);

        ResponseEntity<OutboxEventResponse> retryResponse = restTemplate.exchange(
                "/api/outbox-events/{id}/retry",
                HttpMethod.POST,
                authorizedEntity(admin),
                OutboxEventResponse.class,
                event.getId()
        );

        assertThat(retryResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(retryResponse.getBody()).isNotNull();
        assertThat(retryResponse.getBody().getStatus()).isEqualTo("PENDING");
        assertThat(retryResponse.getBody().getRetryCount()).isEqualTo(1);
        OutboxEvent retried = outboxEventRepository.findById(event.getId())
                .orElseThrow(() -> new AssertionError("expected outbox event"));
        assertThat(retried.getNextRetryAt()).isNotNull();
        assertThat(retried.getProcessedAt()).isNull();
    }

    @Test
    void shouldRejectRetryForNonFailedOutboxEventAndProtectRetryApi() throws Exception {
        AuthResponse admin = login("admin", "admin123");
        AuthResponse risk = login("risk", "risk123");
        OutboxEvent pending = outboxEventPublisher.publish(
                OutboxEventTypes.ORDER_PAID,
                "ORDER",
                "retry-protected",
                "{}",
                1
        );

        ResponseEntity<String> nonFailed = restTemplate.exchange(
                "/api/outbox-events/{id}/retry",
                HttpMethod.POST,
                authorizedEntity(admin),
                String.class,
                pending.getId()
        );
        int noTokenStatus = postWithoutTokenStatus("/api/outbox-events/" + pending.getId() + "/retry");
        ResponseEntity<String> riskOfficer = restTemplate.exchange(
                "/api/outbox-events/{id}/retry",
                HttpMethod.POST,
                authorizedEntity(risk),
                String.class,
                pending.getId()
        );

        assertThat(nonFailed.getStatusCodeValue()).isEqualTo(400);
        assertThat(noTokenStatus).isEqualTo(401);
        assertThat(riskOfficer.getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    void shouldBatchRetryFailedOutboxEventsAndReturnSummary() {
        AuthResponse admin = login("admin", "admin123");
        OutboxEvent first = outboxEventPublisher.publish("UNSUPPORTED_BATCH_EVENT_A", "TEST", "batch-a", "{}", 1);
        OutboxEvent second = outboxEventPublisher.publish("UNSUPPORTED_BATCH_EVENT_B", "TEST", "batch-b", "{}", 1);
        markOutboxEventFailed(first);
        markOutboxEventFailed(second);
        long failedBeforeRetry = outboxEventRepository.findAll().stream()
                .filter(event -> event.getStatus() == OutboxEventStatus.FAILED)
                .count();

        ResponseEntity<OutboxEventSummaryResponse> summaryResponse = restTemplate.exchange(
                "/api/outbox-events/summary",
                HttpMethod.GET,
                authorizedEntity(admin),
                OutboxEventSummaryResponse.class
        );
        ResponseEntity<OutboxRetryResponse> retryResponse = restTemplate.exchange(
                "/api/outbox-events/retry-failed",
                HttpMethod.POST,
                authorizedEntity(admin),
                OutboxRetryResponse.class
        );

        assertThat(summaryResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(summaryResponse.getBody()).isNotNull();
        assertThat(summaryResponse.getBody().getTotalCount()).isGreaterThan(0);
        assertThat(summaryResponse.getBody().getFailedCount()).isGreaterThanOrEqualTo(2);
        assertThat(summaryResponse.getBody().getFailureRate()).isBetween(0.0, 1.0);
        assertThat(summaryResponse.getBody().getEventCountByType()).containsKey("UNSUPPORTED_BATCH_EVENT_A");
        assertThat(summaryResponse.getBody().getEventCountByStatus()).containsKey("FAILED");
        assertThat(retryResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(retryResponse.getBody()).isNotNull();
        assertThat(retryResponse.getBody().getEnqueuedCount()).isEqualTo((int) failedBeforeRetry);
        assertThat(outboxEventRepository.findAll().stream()
                .filter(event -> event.getStatus() == OutboxEventStatus.FAILED)
                .count()).isZero();
    }

    @Test
    void shouldProtectOutboxManagementApisWithAdminRole() {
        AuthResponse risk = login("risk", "risk123");
        AuthResponse admin = login("admin", "admin123");

        ResponseEntity<String> noToken = restTemplate.getForEntity("/api/outbox-events", String.class);
        ResponseEntity<String> riskOfficer = restTemplate.exchange(
                "/api/outbox-events",
                HttpMethod.GET,
                authorizedEntity(risk),
                String.class
        );
        OutboxEventPageResponse adminPage = fetchOutboxPage(admin, "/api/outbox-events?page=0&size=5");

        assertThat(noToken.getStatusCodeValue()).isEqualTo(401);
        assertThat(riskOfficer.getStatusCodeValue()).isEqualTo(403);
        assertThat(adminPage.getPage()).isEqualTo(0);
        assertThat(adminPage.getSize()).isEqualTo(5);
    }

    @Test
    void shouldExposeOpenApiDocsAnonymouslyAndKeepProtectedApisSecured() {
        ResponseEntity<String> apiDocs = restTemplate.getForEntity("/v3/api-docs", String.class);
        ResponseEntity<String> cacheStatsWithoutToken = restTemplate.getForEntity("/api/cache/train-search", String.class);

        assertThat(apiDocs.getStatusCodeValue()).isEqualTo(200);
        assertThat(apiDocs.getBody()).contains("铁路客运票务与风控运营管理系统 API");
        assertThat(cacheStatsWithoutToken.getStatusCodeValue()).isEqualTo(401);
    }

    @Test
    void shouldHandleFailedPaymentAndRejectInvalidPaymentCreation() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3131L;
        OrderResponse pending = createOrder(userId, train, "PaymentFailedUser");
        PaymentResponse payment = createPayment(pending.getId(), "pay-failed-" + System.nanoTime());
        long riskCountBeforeCallback = countRisksForUser(userId);

        PaymentResponse failed = callbackPayment(payment.getPaymentNo(), "callback-failed-" + System.nanoTime(), false);
        long riskCountAfterCallback = countRisksForUser(userId);
        TicketOrder orderAfterFailedPayment = ticketOrderRepository.findById(pending.getId())
                .orElseThrow(() -> new AssertionError("expected order"));
        ResponseEntity<String> successAfterFailed = callbackPaymentRaw(
                payment.getPaymentNo(),
                "callback-success-after-failed-" + System.nanoTime(),
                true
        );

        OrderResponse closed = closeOrder(createOrder(3132L, train, "PaymentClosedUser").getId());
        OrderResponse refunded = refundOrder(createPaidOrder(3133L, train, "PaymentRefundedUser").getId());
        ResponseEntity<String> createForClosed = createPaymentRaw(closed.getId(), "pay-closed-" + System.nanoTime());
        ResponseEntity<String> createForRefunded = createPaymentRaw(refunded.getId(), "pay-refunded-" + System.nanoTime());

        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(orderAfterFailedPayment.getStatus().name()).isEqualTo("PENDING_PAYMENT");
        assertThat(riskCountAfterCallback).isEqualTo(riskCountBeforeCallback);
        assertThat(successAfterFailed.getStatusCodeValue()).isEqualTo(400);
        assertThat(createForClosed.getStatusCodeValue()).isEqualTo(400);
        assertThat(createForRefunded.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldPageAndFilterPaymentRecords() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3134L;
        PaymentResponse pending = createPayment(
                createOrder(userId, train, "PaymentPagePending").getId(),
                "pay-page-pending-" + System.nanoTime()
        );
        PaymentResponse success = callbackPayment(
                createPayment(createOrder(userId, train, "PaymentPageSuccess").getId(), "pay-page-success-" + System.nanoTime()).getPaymentNo(),
                "callback-page-success-" + System.nanoTime(),
                true
        );
        PaymentResponse failed = callbackPayment(
                createPayment(createOrder(userId, train, "PaymentPageFailed").getId(), "pay-page-failed-" + System.nanoTime()).getPaymentNo(),
                "callback-page-failed-" + System.nanoTime(),
                false
        );

        PaymentPageResponse orderPage = fetchPaymentPage("/api/payments?orderId={orderId}", pending.getOrderId());
        PaymentPageResponse firstPage = fetchPaymentPage("/api/payments?page=0&size=1");
        PaymentPageResponse successPage = fetchPaymentPage("/api/payments?status=SUCCESS");
        PaymentPageResponse failedPage = fetchPaymentPage("/api/payments?status=FAILED");
        PaymentPageResponse paymentNoPage = fetchPaymentPage("/api/payments?paymentNo={paymentNo}", success.getPaymentNo().substring(0, 8));
        ResponseEntity<String> invalidStatus = restTemplate.getForEntity("/api/payments?status=UNKNOWN", String.class);

        assertThat(orderPage.getContent()).extracting(PaymentResponse::getId).contains(pending.getId());
        assertThat(firstPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(firstPage.getPage()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(1);
        assertThat(successPage.getContent()).extracting(PaymentResponse::getStatus).contains("SUCCESS");
        assertThat(failedPage.getContent()).extracting(PaymentResponse::getStatus).contains("FAILED");
        assertThat(paymentNoPage.getContent()).extracting(PaymentResponse::getPaymentNo).contains(success.getPaymentNo());
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(invalidStatus.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldCloseExpiredPendingOrderAndReleaseInventory() {
        TrainSearchResponse before = firstTrainInventory();
        OrderResponse created = createOrder(3108L, before, "ExpiredPaymentUser");
        OrderResponse active = createOrder(3114L, before, "ActivePaymentUser");
        TicketOrder order = ticketOrderRepository.findById(created.getId())
                .orElseThrow(() -> new AssertionError("expected order"));
        order.setPaymentDeadlineAt(LocalDateTime.now().minusMinutes(1));
        ticketOrderRepository.save(order);

        OrderResponse[] closedOrders = restTemplate.postForObject("/api/orders/close-expired", null, OrderResponse[].class);
        TrainSearchResponse afterClose = findInventory(before.getInventoryId());

        assertThat(closedOrders).isNotNull();
        assertThat(Arrays.asList(closedOrders))
                .extracting(OrderResponse::getId)
                .contains(created.getId());
        assertThat(Arrays.asList(closedOrders))
                .extracting(OrderResponse::getId)
                .doesNotContain(active.getId());
        TicketOrder activeOrder = ticketOrderRepository.findById(active.getId())
                .orElseThrow(() -> new AssertionError("expected active order"));
        assertThat(activeOrder.getStatus().name()).isEqualTo("PENDING_PAYMENT");
        assertThat(afterClose.getRemainingSeats()).isEqualTo(before.getRemainingSeats() - 1);
    }

    @Test
    void shouldGenerateAndHandleRiskEvents() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3103L;

        createPaidOrder(userId, train, "RiskUserA");
        createPaidOrder(userId, train, "RiskUserB");
        createPaidOrder(userId, train, "RiskUserC");

        List<RiskEventResponse> risks = fetchRisks("/api/risks?userId={userId}&size=20", userId);
        RiskEventResponse unhandledRisk = risks.stream()
                .filter(risk -> Long.valueOf(userId).equals(risk.getUserId()))
                .filter(risk -> !risk.getHandled())
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected unhandled risk event"));

        RiskEventResponse handled = handleRisk(unhandledRisk.getId(), login("risk", "risk123"));

        assertThat(risks)
                .extracting(RiskEventResponse::getRiskType)
                .containsAnyOf("RAPID_PURCHASE", "HIGH_AMOUNT");
        assertThat(handled.getHandled()).isTrue();
    }

    @Test
    void shouldHandleRiskWorkflowWithStatusRemarkHistoryAndDashboardCount() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3140L;
        createPaidOrder(userId, train, "RiskWorkflowA");
        createPaidOrder(userId, train, "RiskWorkflowB");
        createPaidOrder(userId, train, "RiskWorkflowC");

        RiskEventResponse pendingRisk = fetchRisks("/api/risks?status=PENDING").stream()
                .filter(risk -> Long.valueOf(userId).equals(risk.getUserId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected pending risk event"));
        DashboardSummary beforeHandle = restTemplate.getForObject("/api/dashboard/summary", DashboardSummary.class);

        RiskEventResponse confirmed = handleRisk(
                pendingRisk.getId(),
                login("admin", "admin123"),
                "CONFIRMED",
                "短时间多次购票，确认存在异常购票行为"
        );
        DashboardSummary afterHandle = restTemplate.getForObject("/api/dashboard/summary", DashboardSummary.class);
        List<RiskEventHandleRecordResponse> firstRecords = fetchRiskHandleRecords(pendingRisk.getId());
        OperationLog[] logs = latestLogs(login("admin", "admin123"));

        assertThat(pendingRisk.getStatus()).isEqualTo("PENDING");
        assertThat(pendingRisk.getHandled()).isFalse();
        assertThat(pendingRisk.getScene()).isEqualTo("ORDER_CREATED");
        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getHandled()).isTrue();
        assertThat(confirmed.getHandleRemark()).isEqualTo("短时间多次购票，确认存在异常购票行为");
        assertThat(confirmed.getHandledBy()).isEqualTo("admin");
        assertThat(confirmed.getHandledAt()).isNotNull();
        assertThat(confirmed.getClosedAt()).isNull();
        assertThat(afterHandle.getUnhandledRiskCount()).isEqualTo(beforeHandle.getUnhandledRiskCount() - 1);
        assertThat(firstRecords).hasSize(1);
        assertThat(firstRecords.get(0).getFromStatus()).isEqualTo("PENDING");
        assertThat(firstRecords.get(0).getToStatus()).isEqualTo("CONFIRMED");
        assertThat(firstRecords.get(0).getRemark()).isEqualTo("短时间多次购票，确认存在异常购票行为");
        assertThat(firstRecords.get(0).getOperatorName()).isEqualTo("admin");
        assertThat(Arrays.stream(logs))
                .anyMatch(log -> "HANDLE_RISK_EVENT".equals(log.getAction())
                        && String.valueOf(pendingRisk.getId()).equals(log.getTargetId()));

        RiskEventResponse closed = handleRisk(
                pendingRisk.getId(),
                login("risk", "risk123"),
                "CLOSED",
                "已完成人工复核并归档"
        );
        List<RiskEventHandleRecordResponse> allRecords = fetchRiskHandleRecords(pendingRisk.getId());
        ResponseEntity<String> duplicateClose = handleRiskRaw(
                pendingRisk.getId(),
                login("risk", "risk123"),
                "CLOSED",
                "重复关闭"
        );

        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(closed.getHandledBy()).isEqualTo("risk");
        assertThat(closed.getClosedAt()).isNotNull();
        assertThat(allRecords).hasSize(2);
        assertThat(allRecords.get(1).getFromStatus()).isEqualTo("CONFIRMED");
        assertThat(allRecords.get(1).getToStatus()).isEqualTo("CLOSED");
        assertThat(duplicateClose.getStatusCodeValue()).isEqualTo(400);
    }

    @Test
    void shouldLetRiskOfficerMarkFalsePositiveAndFilterRisks() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3141L;
        createPaidOrder(userId, train, "FalsePositiveA");
        createPaidOrder(userId, train, "FalsePositiveB");
        createPaidOrder(userId, train, "FalsePositiveC");

        RiskEventResponse pendingRisk = fetchRisks("/api/risks?status=PENDING&scene=ORDER_CREATED").stream()
                .filter(risk -> Long.valueOf(userId).equals(risk.getUserId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected pending risk event"));

        RiskEventResponse falsePositive = handleRisk(
                pendingRisk.getId(),
                login("risk", "risk123"),
                "FALSE_POSITIVE",
                "核对订单后判断为正常购票"
        );
        List<RiskEventResponse> falsePositiveRisks = fetchRisks("/api/risks?status=FALSE_POSITIVE");

        assertThat(falsePositive.getStatus()).isEqualTo("FALSE_POSITIVE");
        assertThat(falsePositive.getHandleRemark()).isEqualTo("核对订单后判断为正常购票");
        assertThat(falsePositive.getHandledBy()).isEqualTo("risk");
        assertThat(falsePositive.getHandledAt()).isNotNull();
        assertThat(falsePositiveRisks)
                .extracting(RiskEventResponse::getId)
                .contains(pendingRisk.getId());
    }

    @Test
    void shouldPageFilterRisksAndExposeRiskSummary() {
        TrainSearchResponse train = firstTrainInventory();
        RiskEventResponse pendingRisk = createPendingRisk(3150L, train, "RiskQueryPending");
        RiskEventResponse confirmedRisk = handleRisk(
                createPendingRisk(3151L, train, "RiskQueryConfirmed").getId(),
                login("admin", "admin123"),
                "CONFIRMED",
                "分页测试确认风险"
        );
        RiskEventResponse falsePositiveRisk = handleRisk(
                createPendingRisk(3152L, train, "RiskQueryFalsePositive").getId(),
                login("risk", "risk123"),
                "FALSE_POSITIVE",
                "分页测试误报"
        );
        RiskEventResponse closedRisk = handleRisk(
                createPendingRisk(3153L, train, "RiskQueryClosed").getId(),
                login("risk", "risk123"),
                "CLOSED",
                "分页测试关闭"
        );
        String today = LocalDate.now().toString();

        RiskEventPageResponse defaultPage = fetchRiskPage("/api/risks?page=0&size=2");
        RiskEventPageResponse pendingPage = fetchRiskPage("/api/risks?status=PENDING&userId={userId}", pendingRisk.getUserId());
        RiskEventPageResponse confirmedPage = fetchRiskPage("/api/risks?status=CONFIRMED&userId={userId}", confirmedRisk.getUserId());
        RiskEventPageResponse falsePositivePage = fetchRiskPage("/api/risks?status=FALSE_POSITIVE&userId={userId}", falsePositiveRisk.getUserId());
        RiskEventPageResponse closedPage = fetchRiskPage("/api/risks?status=CLOSED&userId={userId}", closedRisk.getUserId());
        RiskEventPageResponse scenePage = fetchRiskPage("/api/risks?scene=ORDER_CREATED&userId={userId}", pendingRisk.getUserId());
        RiskEventPageResponse orderNoPage = fetchRiskPage("/api/risks?orderNo={orderNo}", pendingRisk.getOrderNo());
        RiskEventPageResponse datePage = fetchRiskPage(
                "/api/risks?userId={userId}&fromDate={fromDate}&toDate={toDate}",
                pendingRisk.getUserId(),
                today,
                today
        );

        assertThat(defaultPage.getPage()).isEqualTo(0);
        assertThat(defaultPage.getSize()).isEqualTo(2);
        assertThat(defaultPage.getContent()).hasSizeLessThanOrEqualTo(2);
        assertThat(defaultPage.getTotalElements()).isGreaterThanOrEqualTo(defaultPage.getContent().size());
        assertThat(defaultPage.getTotalPages()).isGreaterThanOrEqualTo(1);
        assertThat(defaultPage.isFirst()).isTrue();

        assertThat(pendingPage.getContent()).extracting(RiskEventResponse::getStatus).containsOnly("PENDING");
        assertThat(confirmedPage.getContent()).extracting(RiskEventResponse::getStatus).containsOnly("CONFIRMED");
        assertThat(falsePositivePage.getContent()).extracting(RiskEventResponse::getStatus).containsOnly("FALSE_POSITIVE");
        assertThat(closedPage.getContent()).extracting(RiskEventResponse::getStatus).containsOnly("CLOSED");
        assertThat(scenePage.getContent()).extracting(RiskEventResponse::getScene).containsOnly("ORDER_CREATED");
        assertThat(orderNoPage.getContent()).extracting(RiskEventResponse::getId).contains(pendingRisk.getId());
        assertThat(datePage.getContent()).extracting(RiskEventResponse::getId).contains(pendingRisk.getId());

        assertThat(restTemplate.getForEntity("/api/risks?status=UNKNOWN", String.class).getStatusCodeValue()).isEqualTo(400);
        assertThat(restTemplate.getForEntity("/api/risks?page=-1", String.class).getStatusCodeValue()).isEqualTo(400);
        assertThat(restTemplate.getForEntity("/api/risks?size=0", String.class).getStatusCodeValue()).isEqualTo(400);

        RiskSummaryResponse summary = restTemplate.getForObject("/api/risks/summary", RiskSummaryResponse.class);
        assertThat(summary).isNotNull();
        assertThat(summary.getTotalRiskCount()).isGreaterThanOrEqualTo(4);
        assertThat(summary.getPendingRiskCount()).isGreaterThan(0);
        assertThat(summary.getConfirmedRiskCount()).isGreaterThan(0);
        assertThat(summary.getFalsePositiveRiskCount()).isGreaterThan(0);
        assertThat(summary.getClosedRiskCount()).isGreaterThan(0);
        assertThat(Double.isFinite(summary.getPendingRate())).isTrue();
        assertThat(Double.isFinite(summary.getConfirmedRate())).isTrue();
        assertThat(Double.isFinite(summary.getFalsePositiveRate())).isTrue();
        assertThat(Double.isFinite(summary.getClosedRate())).isTrue();
        assertThat(Double.isFinite(summary.getHandlingCompletionRate())).isTrue();
        assertThat(Double.isFinite(summary.getAverageHandleMinutes())).isTrue();
        assertThat(summary.getRiskCountByScene()).containsKey("ORDER_CREATED");
        assertThat(summary.getRiskCountByStatus()).containsKey("PENDING");
        assertThat(summary.getRiskCountByStatus().get("CONFIRMED")).isEqualTo(summary.getConfirmedRiskCount());
        assertThat(summary.getRiskCountByStatus().get("FALSE_POSITIVE")).isEqualTo(summary.getFalsePositiveRiskCount());
        assertThat(summary.getRiskCountByStatus().get("CLOSED")).isEqualTo(summary.getClosedRiskCount());
    }

    @Test
    void shouldProtectRiskHandlingWithRole() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3105L;
        createPaidOrder(userId, train, "ProtectedRiskA");
        createPaidOrder(userId, train, "ProtectedRiskB");

        RiskEventResponse risk = fetchRisks("/api/risks?userId={userId}&size=20", userId).stream()
                .filter(item -> Long.valueOf(userId).equals(item.getUserId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected risk event"));

        ResponseEntity<String> unauthenticated = restTemplate.getForEntity("/api/auth/me", String.class);
        ResponseEntity<String> forbidden = restTemplate.exchange(
                "/api/risks/{id}/handle",
                HttpMethod.POST,
                authorizedEntity(login("ops", "ops123")),
                String.class,
                risk.getId()
        );

        assertThat(unauthenticated.getStatusCodeValue()).isEqualTo(401);
        assertThat(forbidden.getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    void shouldAuthenticateWithJwtAndBcryptPasswords() throws Exception {
        AuthResponse admin = login("admin", "admin123");

        int failedLoginStatus = postLoginStatus("admin", "bad-password");
        ResponseEntity<String> health = restTemplate.getForEntity("/api/health", String.class);
        ResponseEntity<AuthResponse> me = restTemplate.exchange(
                "/api/auth/me",
                HttpMethod.GET,
                authorizedEntity(admin),
                AuthResponse.class
        );
        ResponseEntity<String> noToken = restTemplate.getForEntity("/api/cache/train-search", String.class);
        ResponseEntity<String> invalidToken = restTemplate.exchange(
                "/api/cache/train-search",
                HttpMethod.GET,
                authorizedEntity("invalid.token.value"),
                String.class
        );
        ResponseEntity<TrainSearchCacheStats> cacheStats = restTemplate.exchange(
                "/api/cache/train-search",
                HttpMethod.GET,
                authorizedEntity(admin),
                TrainSearchCacheStats.class
        );

        String passwordHash = appUserRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("expected admin user"))
                .getPasswordHash();

        assertThat(admin.getToken()).isNotBlank();
        assertThat(admin.getToken().split("\\.")).hasSize(3);
        assertThat(admin.getExpiresAt()).isNotNull();
        assertThat(failedLoginStatus).isEqualTo(401);
        assertThat(passwordHash).startsWith("$2");
        assertThat(passwordEncoder.matches("admin123", passwordHash)).isTrue();
        assertThat(health.getStatusCodeValue()).isEqualTo(200);
        assertThat(me.getStatusCodeValue()).isEqualTo(200);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().getUsername()).isEqualTo("admin");
        assertThat(noToken.getStatusCodeValue()).isEqualTo(401);
        assertThat(invalidToken.getStatusCodeValue()).isEqualTo(401);
        assertThat(cacheStats.getStatusCodeValue()).isEqualTo(200);
        assertThat(cacheStats.getBody()).isNotNull();
    }

    @Test
    void shouldExposePassengerApisWithOwnershipBoundaries() {
        AuthResponse passenger1 = login("passenger1", "123456");
        AuthResponse passenger2 = login("passenger2", "123456");
        AuthResponse admin = login("admin", "admin123");
        Long passenger1Id = appUserRepository.findByUsername("passenger1")
                .orElseThrow(() -> new AssertionError("expected passenger1"))
                .getId();
        Long passenger2Id = appUserRepository.findByUsername("passenger2")
                .orElseThrow(() -> new AssertionError("expected passenger2"))
                .getId();

        OrderResponse passenger1Pending = createPassengerOrder(passenger1, firstTrainInventory(), "Passenger One A");
        OrderResponse passenger1CloseTarget = createPassengerOrder(passenger1, firstTrainInventory(), "Passenger One B");
        OrderResponse passenger2Pending = createPassengerOrder(passenger2, firstTrainInventory(), "Passenger Two A");

        PassengerSummaryResponse summary = restTemplate.exchange(
                "/api/passenger/summary",
                HttpMethod.GET,
                authorizedEntity(passenger1),
                PassengerSummaryResponse.class
        ).getBody();
        OrderPageResponse passenger1Orders = restTemplate.exchange(
                "/api/passenger/orders?size=50",
                HttpMethod.GET,
                authorizedEntity(passenger1),
                OrderPageResponse.class
        ).getBody();
        ResponseEntity<String> payOtherOrder = restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                String.class,
                passenger2Pending.getId()
        );
        OrderResponse closed = restTemplate.exchange(
                "/api/passenger/orders/{id}/close",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                OrderResponse.class,
                passenger1CloseTarget.getId()
        ).getBody();
        OrderResponse paid = restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                OrderResponse.class,
                passenger1Pending.getId()
        ).getBody();
        PaymentPageResponse payments = restTemplate.exchange(
                "/api/passenger/payments?status=SUCCESS&size=50",
                HttpMethod.GET,
                authorizedEntity(passenger1),
                PaymentPageResponse.class
        ).getBody();
        OrderResponse refunded = restTemplate.exchange(
                "/api/passenger/orders/{id}/refund",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                OrderResponse.class,
                paid.getId()
        ).getBody();
        RefundPageResponse refunds = restTemplate.exchange(
                "/api/passenger/refunds?size=50",
                HttpMethod.GET,
                authorizedEntity(passenger1),
                RefundPageResponse.class
        ).getBody();
        OrderPageResponse adminOrders = restTemplate.exchange(
                "/api/orders?userId={userId}&size=50",
                HttpMethod.GET,
                authorizedEntity(admin),
                OrderPageResponse.class,
                passenger1Id
        ).getBody();

        assertThat(passenger1.getRole()).isEqualTo("USER");
        assertThat(passenger1Pending.getUserId()).isEqualTo(passenger1Id);
        assertThat(passenger2Pending.getUserId()).isEqualTo(passenger2Id);
        assertThat(summary).isNotNull();
        assertThat(summary.getPendingPaymentOrderCount()).isGreaterThanOrEqualTo(1);
        assertThat(passenger1Orders).isNotNull();
        assertThat(passenger1Orders.getContent()).isNotEmpty();
        assertThat(passenger1Orders.getContent()).allSatisfy(order -> assertThat(order.getUserId()).isEqualTo(passenger1Id));
        assertThat(passenger1Orders.getContent()).extracting(OrderResponse::getId).doesNotContain(passenger2Pending.getId());
        assertThat(payOtherOrder.getStatusCodeValue()).isEqualTo(403);
        assertThat(closed).isNotNull();
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(paid).isNotNull();
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(payments).isNotNull();
        assertThat(payments.getContent()).isNotEmpty();
        assertThat(payments.getContent()).allSatisfy(payment -> assertThat(payment.getUserId()).isEqualTo(passenger1Id));
        assertThat(payments.getContent()).extracting(PaymentResponse::getOrderId).contains(paid.getId());
        assertThat(refunded).isNotNull();
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(refunds).isNotNull();
        assertThat(refunds.getContent()).isNotEmpty();
        assertThat(refunds.getContent()).allSatisfy(refund -> assertThat(refund.getUserId()).isEqualTo(passenger1Id));
        assertThat(refunds.getContent()).extracting(RefundResponse::getOrderId).contains(refunded.getId());
        assertThat(adminOrders).isNotNull();
        assertThat(adminOrders.getContent()).extracting(OrderResponse::getId).contains(passenger1Pending.getId());

        assertThat(userGetStatus(passenger1, "/api/orders")).isEqualTo(403);
        assertThat(userGetStatus(passenger1, "/api/risks")).isEqualTo(403);
        assertThat(userGetStatus(passenger1, "/api/outbox-events")).isEqualTo(403);
        assertThat(userGetStatus(passenger1, "/api/logs")).isEqualTo(403);
    }

    @Test
    void shouldCreateAndSecurePassengerNotifications() {
        AuthResponse passenger1 = login("passenger1", "123456");
        AuthResponse passenger2 = login("passenger2", "123456");
        AuthResponse admin = login("admin", "admin123");
        Long passenger1Id = appUserRepository.findByUsername("passenger1")
                .orElseThrow(() -> new AssertionError("expected passenger1"))
                .getId();
        Long passenger2Id = appUserRepository.findByUsername("passenger2")
                .orElseThrow(() -> new AssertionError("expected passenger2"))
                .getId();

        OrderResponse passenger1Order = createPassengerOrder(passenger1, firstTrainInventory(), "Notify Passenger One");
        OrderResponse passenger2Order = createPassengerOrder(passenger2, firstTrainInventory(), "Notify Passenger Two");

        NotificationPageResponse passenger1Notifications = fetchPassengerNotifications(passenger1, "/api/passenger/notifications?size=100");
        NotificationPageResponse passenger2Notifications = fetchPassengerNotifications(passenger2, "/api/passenger/notifications?size=100");
        NotificationResponse passenger2Notification = passenger2Notifications.getContent().stream()
                .filter(notification -> passenger2Order.getOrderNo().equals(notification.getOrderNo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected passenger2 notification"));
        ResponseEntity<String> markOtherRead = restTemplate.exchange(
                "/api/passenger/notifications/{id}/read",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                String.class,
                passenger2Notification.getId()
        );

        OrderResponse paid = restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                OrderResponse.class,
                passenger1Order.getId()
        ).getBody();
        OrderResponse refunded = restTemplate.exchange(
                "/api/passenger/orders/{id}/refund",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                OrderResponse.class,
                passenger1Order.getId()
        ).getBody();
        RefundRecord refund = refundRecordRepository.findByOrderIdAndStatus(passenger1Order.getId(), RefundStatus.PENDING)
                .orElseThrow(() -> new AssertionError("expected pending refund"));
        callbackRefund(refund.getRefundNo(), "notify-refund-" + System.nanoTime(), true);

        NotificationPageResponse afterFlow = fetchPassengerNotifications(passenger1, "/api/passenger/notifications?size=100");
        NotificationResponse unread = afterFlow.getContent().stream()
                .filter(notification -> NotificationStatus.UNREAD.name().equals(notification.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected unread notification"));
        NotificationResponse markedRead = restTemplate.exchange(
                "/api/passenger/notifications/{id}/read",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                NotificationResponse.class,
                unread.getId()
        ).getBody();
        NotificationSummaryResponse readAllSummary = restTemplate.exchange(
                "/api/passenger/notifications/read-all",
                HttpMethod.POST,
                authorizedEntity(passenger1),
                NotificationSummaryResponse.class
        ).getBody();
        NotificationPageResponse adminNotifications = restTemplate.exchange(
                "/api/notifications?orderNo={orderNo}&size=50",
                HttpMethod.GET,
                authorizedEntity(admin),
                NotificationPageResponse.class,
                passenger1Order.getOrderNo()
        ).getBody();
        NotificationSummaryResponse adminSummary = restTemplate.exchange(
                "/api/notifications/summary",
                HttpMethod.GET,
                authorizedEntity(admin),
                NotificationSummaryResponse.class
        ).getBody();

        assertThat(passenger1Notifications.getContent()).allSatisfy(notification -> assertThat(notification.getUserId()).isEqualTo(passenger1Id));
        assertThat(passenger1Notifications.getContent()).extracting(NotificationResponse::getOrderNo).doesNotContain(passenger2Order.getOrderNo());
        assertThat(markOtherRead.getStatusCodeValue()).isEqualTo(403);
        assertThat(paid).isNotNull();
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(refunded).isNotNull();
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(afterFlow.getContent()).extracting(NotificationResponse::getType)
                .contains(
                        NotificationType.ORDER_CREATED.name(),
                        NotificationType.PAYMENT_SUCCEEDED.name(),
                        NotificationType.TICKET_ISSUED.name(),
                        NotificationType.ORDER_REFUNDED.name(),
                        NotificationType.REFUND_SUCCEEDED.name()
                );
        assertThat(afterFlow.getContent()).allSatisfy(notification -> assertThat(notification.getUserId()).isEqualTo(passenger1Id));
        assertThat(markedRead).isNotNull();
        assertThat(markedRead.getStatus()).isEqualTo(NotificationStatus.READ.name());
        assertThat(readAllSummary).isNotNull();
        assertThat(readAllSummary.getUnreadCount()).isZero();
        assertThat(adminNotifications).isNotNull();
        assertThat(adminNotifications.getContent()).extracting(NotificationResponse::getOrderNo).contains(passenger1Order.getOrderNo());
        assertThat(adminSummary).isNotNull();
        assertThat(adminSummary.getTotalCount()).isGreaterThan(0);
        assertThat(adminSummary.getCountByType()).containsKey(NotificationType.ORDER_CREATED.name());
        assertThat(userGetStatus(passenger1, "/api/notifications")).isEqualTo(403);
        assertThat(passenger2Notifications.getContent()).allSatisfy(notification -> assertThat(notification.getUserId()).isEqualTo(passenger2Id));
    }

    @Test
    void shouldSearchAdminBusinessLinksAndProtectSensitivePassengerData() {
        AuthResponse passenger = login("passenger1", "123456");
        AuthResponse admin = login("admin", "admin123");
        AuthResponse operator = login("ops", "ops123");
        String suffix = String.valueOf(Math.abs(System.nanoTime()));
        suffix = suffix.substring(Math.max(0, suffix.length() - 8));

        PassengerTravelerRequest travelerRequest = new PassengerTravelerRequest();
        travelerRequest.setPassengerName("Search Traveler " + suffix);
        travelerRequest.setIdType("ID_CARD");
        travelerRequest.setIdNo("33010119990101" + suffix.substring(0, 4));
        travelerRequest.setPhone("139" + suffix.substring(0, 8));
        travelerRequest.setDefaultTraveler(true);
        PassengerTravelerResponse traveler = restTemplate.exchange(
                "/api/passenger/travelers",
                HttpMethod.POST,
                authorizedEntity(passenger, travelerRequest),
                PassengerTravelerResponse.class
        ).getBody();

        OrderResponse order = createPassengerOrder(passenger, firstTrainInventory(), travelerRequest.getPassengerName());
        OrderResponse paid = restTemplate.exchange(
                "/api/passenger/orders/{id}/pay",
                HttpMethod.POST,
                authorizedEntity(passenger),
                OrderResponse.class,
                order.getId()
        ).getBody();
        TicketRecord ticket = ticketRecordRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AssertionError("expected ticket"));
        PaymentRecord payment = paymentRecordRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .filter(record -> PaymentStatus.SUCCESS.equals(record.getStatus()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected payment"));
        OrderResponse refunded = restTemplate.exchange(
                "/api/passenger/orders/{id}/refund",
                HttpMethod.POST,
                authorizedEntity(passenger),
                OrderResponse.class,
                order.getId()
        ).getBody();
        RefundRecord refund = refundRecordRepository.findByOrderIdAndStatus(order.getId(), RefundStatus.PENDING)
                .orElseThrow(() -> new AssertionError("expected refund"));
        NotificationRecord notification = notificationRecordRepository.findAll().stream()
                .filter(record -> order.getOrderNo().equals(record.getOrderNo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected notification"));

        AdminGlobalSearchResponse orderSearch = search(admin,
                "/api/search?keyword={keyword}&types=ORDER&limitPerType=3&includeTrace=true",
                order.getOrderNo());
        AdminGlobalSearchResponse ticketSearch = search(admin,
                "/api/search?keyword={keyword}&types=TICKET&limitPerType=3",
                ticket.getTicketNo());
        AdminGlobalSearchResponse paymentSearch = search(admin,
                "/api/search?keyword={keyword}&types=PAYMENT&limitPerType=3",
                payment.getPaymentNo());
        AdminGlobalSearchResponse refundSearch = search(admin,
                "/api/search?keyword={keyword}&types=REFUND&limitPerType=3",
                refund.getRefundNo());
        AdminGlobalSearchResponse notificationSearch = search(operator,
                "/api/search?keyword={keyword}&types=NOTIFICATION&limitPerType=3",
                notification.getNotificationNo());
        ResponseEntity<String> travelerSearch = restTemplate.exchange(
                "/api/search?keyword={keyword}&types=TRAVELER&limitPerType=3",
                HttpMethod.GET,
                authorizedEntity(admin),
                String.class,
                travelerRequest.getPassengerName()
        );
        ResponseEntity<String> invalidKeyword = restTemplate.exchange(
                "/api/search?keyword=A",
                HttpMethod.GET,
                authorizedEntity(admin),
                String.class
        );
        ResponseEntity<String> noToken = restTemplate.getForEntity("/api/search?keyword={keyword}", String.class, order.getOrderNo());

        SearchResultGroupResponse orderGroup = group(orderSearch, "ORDER");
        SearchResultItemResponse orderItem = orderGroup.getItems().get(0);

        assertThat(traveler).isNotNull();
        assertThat(paid).isNotNull();
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(refunded).isNotNull();
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(orderSearch.getKeyword()).isEqualTo(order.getOrderNo());
        assertThat(orderSearch.getTotalCount()).isGreaterThan(0);
        assertThat(orderSearch.getGroups()).hasSize(1);
        assertThat(orderGroup.getCount()).isGreaterThan(0);
        assertThat(orderGroup.getItems()).extracting(SearchResultItemResponse::getOrderNo).contains(order.getOrderNo());
        assertThat(orderItem.getOrderId()).isEqualTo(order.getId());
        assertThat(orderItem.getDetailAction()).isEqualTo("ORDER_DETAIL");
        assertThat(orderItem.getTrace()).isNotEmpty();
        assertThat(group(ticketSearch, "TICKET").getItems()).extracting(SearchResultItemResponse::getTicketNo).contains(ticket.getTicketNo());
        assertThat(group(paymentSearch, "PAYMENT").getItems()).extracting(SearchResultItemResponse::getPaymentNo).contains(payment.getPaymentNo());
        assertThat(group(refundSearch, "REFUND").getItems()).extracting(SearchResultItemResponse::getRefundNo).contains(refund.getRefundNo());
        assertThat(group(notificationSearch, "NOTIFICATION").getItems()).extracting(SearchResultItemResponse::getNotificationNo)
                .contains(notification.getNotificationNo());
        assertThat(travelerSearch.getStatusCodeValue()).isEqualTo(200);
        assertThat(travelerSearch.getBody()).doesNotContain(travelerRequest.getIdNo());
        assertThat(travelerSearch.getBody()).doesNotContain(travelerRequest.getPhone());
        assertThat(invalidKeyword.getStatusCodeValue()).isEqualTo(400);
        assertThat(noToken.getStatusCodeValue()).isEqualTo(401);
        assertThat(userGetStatus(passenger, "/api/search?keyword=" + order.getOrderNo())).isEqualTo(403);
    }

    @Test
    void shouldCacheTrainSearchAndEvictAfterInventoryChange() {
        AuthResponse admin = login("admin", "admin123");
        clearTrainSearchCache(admin);

        List<TrainSearchResponse> firstSearch = searchToday();
        List<TrainSearchResponse> secondSearch = searchToday();
        TrainSearchCacheStats afterSecondSearch = trainSearchCacheStats(admin);

        assertThat(firstSearch).isNotEmpty();
        assertThat(secondSearch).isNotEmpty();
        assertThat(afterSecondSearch.getCacheMode()).isEqualTo("local");
        assertThat(afterSecondSearch.getConfiguredMode()).isEqualTo("local");
        assertThat(afterSecondSearch.getTtlSeconds()).isGreaterThan(0);
        assertThat(afterSecondSearch.getEntryCount()).isGreaterThan(0);
        assertThat(afterSecondSearch.getHitCount()).isGreaterThan(0);

        OrderResponse created = createOrder(3106L, secondSearch.get(0), "CacheEvictUser");
        TrainSearchCacheStats afterOrder = trainSearchCacheStats(admin);

        assertThat(afterOrder.getEntryCount()).isEqualTo(0);
        assertThat(afterOrder.getEvictCount()).isGreaterThan(0);

        searchToday();
        searchToday();
        assertThat(trainSearchCacheStats(admin).getEntryCount()).isGreaterThan(0);
        OrderResponse paid = payOrder(created.getId());
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(trainSearchCacheStats(admin).getEntryCount()).isEqualTo(0);

        searchToday();
        searchToday();
        assertThat(trainSearchCacheStats(admin).getEntryCount()).isGreaterThan(0);
        OrderResponse refunded = refundOrder(paid.getId());
        assertThat(refunded.getStatus()).isEqualTo("REFUNDED");
        assertThat(trainSearchCacheStats(admin).getEntryCount()).isEqualTo(0);

        OrderResponse pending = createOrder(3115L, firstTrainInventory(), "CacheCloseUser");
        searchToday();
        searchToday();
        assertThat(trainSearchCacheStats(admin).getEntryCount()).isGreaterThan(0);
        OrderResponse closed = closeOrder(pending.getId());
        assertThat(closed.getStatus()).isEqualTo("CLOSED");
        assertThat(trainSearchCacheStats(admin).getEntryCount()).isEqualTo(0);
    }

    @Test
    void shouldRateLimitHighFrequencyApisWithoutPollutingOtherUsers() {
        AuthResponse admin = login("admin", "admin123");
        RateLimitSummary initialSummary = rateLimitSummary(admin);
        int trainSearchLimit = initialSummary.getRules().get("train-search").getLimit();
        int orderCreateLimit = initialSummary.getRules().get("order-create").getLimit();
        String date = LocalDate.now().toString();
        ResponseEntity<String> anonymousTrainSearch = restTemplate.getForEntity(
                "/api/trains/search?from=BJP&to=SHH&date={date}",
                String.class,
                date
        );

        ResponseEntity<String> trainLimited = null;
        for (int i = 0; i < trainSearchLimit + 1; i++) {
            trainLimited = restTemplate.exchange(
                    "/api/trains/search?from=BJP&to=SHH&date={date}",
                    HttpMethod.GET,
                    authorizedEntity(admin),
                    String.class,
                    date
            );
        }

        ResponseEntity<String> orderLimited = null;
        for (int i = 0; i < orderCreateLimit + 1; i++) {
            orderLimited = createOrderRaw(
                    94001L,
                    999999L,
                    999999L,
                    "RateLimitUser" + i
            );
        }
        ResponseEntity<String> otherUserOrder = createOrderRaw(
                94002L,
                999999L,
                999999L,
                "RateLimitOtherUser"
        );

        RateLimitSummary summary = rateLimitSummary(admin);

        assertThat(anonymousTrainSearch.getStatusCodeValue()).isEqualTo(200);
        assertThat(trainLimited).isNotNull();
        assertThat(trainLimited.getStatusCodeValue()).isEqualTo(429);
        assertThat(orderLimited).isNotNull();
        assertThat(orderLimited.getStatusCodeValue()).isEqualTo(429);
        assertThat(otherUserOrder.getStatusCodeValue()).isNotEqualTo(429);
        assertThat(summary.getMode()).isEqualTo("local");
        assertThat(summary.getConfiguredMode()).isEqualTo("local");
        assertThat(summary.getBlockedCount()).isGreaterThanOrEqualTo(2);
        assertThat(summary.getRules()).containsKeys("train-search", "order-create", "payment-callback", "risk-handle");
        assertThat(rateLimitProperties.getRule("train-search").getLimit()).isEqualTo(trainSearchLimit);
    }

    @Test
    void shouldUseLocalFallbackWhenRedisCacheModeCannotConnect() {
        AuthResponse admin = login("admin", "admin123");
        String originalMode = trainSearchCacheProperties.getMode();
        try {
            trainSearchCacheProperties.setMode("redis");
            ResponseEntity<TrainSearchResponse[]> response = restTemplate.getForEntity(
                    "/api/trains/search?from=BJP&to=SHH&date={date}",
                    TrainSearchResponse[].class,
                    LocalDate.now().toString()
            );
            TrainSearchCacheStats stats = trainSearchCacheStats(admin);

            assertThat(response.getStatusCodeValue()).isEqualTo(200);
            assertThat(stats.getConfiguredMode()).isEqualTo("redis");
            assertThat(stats.getCacheMode()).isIn("redis", "local");
            if (!stats.isRedisAvailable()) {
                assertThat(stats.isLocalFallback()).isTrue();
                assertThat(stats.getCacheMode()).isEqualTo("local");
            }
        } finally {
            trainSearchCacheProperties.setMode(originalMode);
            clearTrainSearchCache(admin);
        }
    }

    @Test
    void shouldListAvailableTrainsWithoutBreakingRouteSearch() {
        String date = LocalDate.now().toString();

        ResponseEntity<TrainSearchResponse[]> availableResponse = restTemplate.getForEntity(
                "/api/trains/available?travelDate={date}&page=0&size=8",
                TrainSearchResponse[].class,
                date
        );
        ResponseEntity<TrainSearchResponse[]> filteredResponse = restTemplate.getForEntity(
                "/api/trains/available?fromStation=BJP&toStation=SHH&travelDate={date}",
                TrainSearchResponse[].class,
                date
        );
        ResponseEntity<TrainSearchResponse[]> originalSearchResponse = restTemplate.getForEntity(
                "/api/trains/search?from=BJP&to=SHH&date={date}",
                TrainSearchResponse[].class,
                date
        );

        assertThat(availableResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(availableResponse.getBody()).isNotNull();
        assertThat(Arrays.asList(availableResponse.getBody()))
                .isNotEmpty()
                .allSatisfy(train -> assertThat(train.getRemainingSeats()).isGreaterThan(0));
        assertThat(filteredResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(filteredResponse.getBody()).isNotNull();
        assertThat(Arrays.asList(filteredResponse.getBody()))
                .isNotEmpty()
                .allSatisfy(train -> assertThat(train.getTrainNo()).isEqualTo("G101"));
        assertThat(originalSearchResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(originalSearchResponse.getBody()).isNotNull();
        assertThat(Arrays.asList(originalSearchResponse.getBody()))
                .isNotEmpty()
                .extracting(TrainSearchResponse::getTrainNo)
                .contains("G101");
    }

    @Test
    void shouldPreventOversellUnderConcurrentPurchase() throws Exception {
        SeatInventory inventory = createSingleSeatInventory();
        int requestCount = 16;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<Future<ResponseEntity<String>>>();

        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            futures.add(executorService.submit(new Callable<ResponseEntity<String>>() {
                @Override
                public ResponseEntity<String> call() throws Exception {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return createOrderRaw(
                            9000L + index,
                            inventory.getTrain().getId(),
                            inventory.getId(),
                            "ConcurrentUser" + index
                    );
                }
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int successCount = 0;
        int failedCount = 0;
        for (Future<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get(10, TimeUnit.SECONDS);
            int status = response.getStatusCodeValue();
            if (status >= 200 && status < 300) {
                successCount++;
            } else {
                failedCount++;
                assertThat(status).isIn(400, 409);
            }
        }
        executorService.shutdownNow();

        SeatInventory latestInventory = seatInventoryRepository.findById(inventory.getId())
                .orElseThrow(() -> new AssertionError("expected inventory"));

        assertThat(successCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(requestCount - 1);
        assertThat(latestInventory.getRemainingSeats()).isEqualTo(0);
        assertThat(ticketOrderRepository.countByInventory_Id(inventory.getId())).isEqualTo(1);
    }

    @Test
    void dashboardShouldExposeOrderAndRiskMetrics() {
        TrainSearchResponse train = firstTrainInventory();
        createOrder(3104L, train, "DashboardPendingUser");
        createPaidOrder(3104L, train, "DashboardPaidUser");
        closeOrder(createOrder(3104L, train, "DashboardClosedUser").getId());
        refundOrder(createPaidOrder(3104L, train, "DashboardRefundedUser").getId());

        DashboardSummary summary = restTemplate.getForObject("/api/dashboard/summary", DashboardSummary.class);

        assertThat(summary.getTotalOrders()).isGreaterThan(0);
        assertThat(summary.getPaidOrders()).isGreaterThan(0);
        assertThat(summary.getTotalOrderCount()).isGreaterThan(0);
        assertThat(summary.getPendingPaymentOrderCount()).isGreaterThan(0);
        assertThat(summary.getPaidOrderCount()).isGreaterThan(0);
        assertThat(summary.getClosedOrderCount()).isGreaterThan(0);
        assertThat(summary.getRefundedOrderCount()).isGreaterThan(0);
        assertThat(summary.getUnhandledRiskCount()).isGreaterThanOrEqualTo(0);
        assertThat(Double.isFinite(summary.getRefundRate())).isTrue();
        assertThat(Double.isFinite(summary.getRiskRate())).isTrue();
        assertThat(summary.getRefundRate()).isGreaterThanOrEqualTo(0.0D);
        assertThat(summary.getRiskRate()).isGreaterThanOrEqualTo(0.0D);
        assertThat(summary.getPopularTrains()).isNotEmpty();
    }

    private OrderResponse createOrder(long userId, TrainSearchResponse train, String passengerName) {
        return createOrder(userId, train, passengerName, "test-" + userId + "-" + System.nanoTime());
    }

    private OrderResponse createPaidOrder(long userId, TrainSearchResponse train, String passengerName) {
        return payOrder(createOrder(userId, train, passengerName).getId());
    }

    private OrderResponse createPaidOrderThroughPayment(long userId, TrainSearchResponse train, String passengerName) {
        OrderResponse pending = createOrder(userId, train, passengerName);
        PaymentResponse payment = createPayment(pending.getId(), "pay-order-" + pending.getId() + "-" + System.nanoTime());
        callbackPayment(payment.getPaymentNo(), "callback-order-" + pending.getId() + "-" + System.nanoTime(), true);
        return fetchOrderPage("/api/orders?orderNo={orderNo}", pending.getOrderNo()).getContent().stream()
                .filter(order -> pending.getId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected order"));
    }

    private OrderResponse createOrder(long userId, TrainSearchResponse train, String passengerName, String requestId) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setRequestId(requestId);
        request.setTrainId(train.getTrainId());
        request.setInventoryId(train.getInventoryId());
        request.setPassengerName(passengerName);
        request.setPassengerIdCard("11010120000101" + userId);
        return restTemplate.postForObject("/api/orders", request, OrderResponse.class);
    }

    private OrderResponse payOrder(Long orderId) {
        return restTemplate.postForObject("/api/orders/{id}/pay", null, OrderResponse.class, orderId);
    }

    private ResponseEntity<String> payOrderRaw(Long orderId) {
        return restTemplate.postForEntity("/api/orders/{id}/pay", null, String.class, orderId);
    }

    private OrderResponse closeOrder(Long orderId) {
        return restTemplate.postForObject("/api/orders/{id}/close", null, OrderResponse.class, orderId);
    }

    private ResponseEntity<String> closeOrderRaw(Long orderId) {
        return restTemplate.postForEntity("/api/orders/{id}/close", null, String.class, orderId);
    }

    private OrderResponse refundOrder(Long orderId) {
        return restTemplate.postForObject("/api/orders/{id}/refund", null, OrderResponse.class, orderId);
    }

    private ResponseEntity<String> refundOrderRaw(Long orderId) {
        return restTemplate.postForEntity("/api/orders/{id}/refund", null, String.class, orderId);
    }

    private OrderPageResponse fetchOrderPage(String url, Object... uriVariables) {
        OrderPageResponse response = restTemplate.getForObject(url, OrderPageResponse.class, uriVariables);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        return response;
    }

    private PaymentResponse createPayment(Long orderId, String requestId) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(orderId);
        request.setRequestId(requestId);
        return restTemplate.postForObject("/api/payments", request, PaymentResponse.class);
    }

    private ResponseEntity<String> createPaymentRaw(Long orderId, String requestId) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrderId(orderId);
        request.setRequestId(requestId);
        return restTemplate.postForEntity("/api/payments", request, String.class);
    }

    private PaymentResponse callbackPayment(String paymentNo, String callbackRequestId, boolean success) {
        PaymentCallbackRequest request = signedPaymentCallbackRequest(paymentNo, callbackRequestId, success);
        return restTemplate.postForObject("/api/payments/callback", request, PaymentResponse.class);
    }

    private ResponseEntity<String> callbackPaymentRaw(String paymentNo, String callbackRequestId, boolean success) {
        PaymentCallbackRequest request = signedPaymentCallbackRequest(paymentNo, callbackRequestId, success);
        return restTemplate.postForEntity("/api/payments/callback", request, String.class);
    }

    private PaymentCallbackRequest signedPaymentCallbackRequest(String paymentNo, String callbackRequestId, boolean success) {
        PaymentRecord record = paymentRecordRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new AssertionError("expected payment"));
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPaymentNo(paymentNo);
        request.setCallbackRequestId(callbackRequestId);
        request.setSuccess(success);
        request.setChannelPaymentNo(success ? "CH_PAY_" + callbackRequestId : null);
        request.setAmount(record.getAmount());
        request.setTimestamp(callbackSignatureService.currentTimestamp());
        request.setMessage(success ? "mock payment success" : "mock payment failed");
        request.setSignature(signPaymentCallback(request));
        return request;
    }

    private String signPaymentCallback(PaymentCallbackRequest request) {
        return callbackSignatureService.sign(
                callbackSignatureService.paymentPlainText(
                        request.getPaymentNo(),
                        request.getCallbackRequestId(),
                        request.getAmount(),
                        request.getSuccess(),
                        request.getTimestamp()
                ),
                paymentCallbackProperties.getCallbackSecret()
        );
    }

    private PaymentPageResponse fetchPaymentPage(String url, Object... uriVariables) {
        PaymentPageResponse response = restTemplate.getForObject(url, PaymentPageResponse.class, uriVariables);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        return response;
    }

    private RefundResponse callbackRefund(String refundNo, String callbackRequestId, boolean success) {
        RefundCallbackRequest request = signedRefundCallbackRequest(refundNo, callbackRequestId, success);
        return restTemplate.postForObject("/api/refunds/callback", request, RefundResponse.class);
    }

    private ResponseEntity<String> callbackRefundRaw(String refundNo, String callbackRequestId, boolean success) {
        RefundCallbackRequest request = signedRefundCallbackRequest(refundNo, callbackRequestId, success);
        return restTemplate.postForEntity("/api/refunds/callback", request, String.class);
    }

    private RefundCallbackRequest signedRefundCallbackRequest(String refundNo, String callbackRequestId, boolean success) {
        RefundRecord record = refundRecordRepository.findByRefundNo(refundNo)
                .orElseThrow(() -> new AssertionError("expected refund"));
        RefundCallbackRequest request = new RefundCallbackRequest();
        request.setRefundNo(refundNo);
        request.setCallbackRequestId(callbackRequestId);
        request.setSuccess(success);
        request.setChannelRefundNo(success ? "CH_RF_" + callbackRequestId : null);
        request.setAmount(record.getAmount());
        request.setTimestamp(callbackSignatureService.currentTimestamp());
        request.setMessage(success ? "mock refund success" : "mock refund failed");
        request.setSignature(signRefundCallback(request));
        return request;
    }

    private String signRefundCallback(RefundCallbackRequest request) {
        return callbackSignatureService.sign(
                callbackSignatureService.refundPlainText(
                        request.getRefundNo(),
                        request.getCallbackRequestId(),
                        request.getAmount(),
                        request.getSuccess(),
                        request.getTimestamp()
                ),
                refundCallbackProperties.getCallbackSecret()
        );
    }

    private RefundPageResponse fetchRefundPage(String url, Object... uriVariables) {
        RefundPageResponse response = restTemplate.getForObject(url, RefundPageResponse.class, uriVariables);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        return response;
    }

    private long countOutboxEvents(String eventType) {
        return outboxEventRepository.findAll().stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .count();
    }

    private void markOutboxEventFailed(OutboxEvent event) {
        event.setStatus(OutboxEventStatus.FAILED);
        event.setRetryCount(event.getMaxRetryCount());
        event.setLastError("test failure");
        event.setUpdatedAt(LocalDateTime.now());
        event.setProcessedAt(event.getUpdatedAt());
        outboxEventRepository.save(event);
    }

    private OutboxEventPageResponse fetchOutboxPage(AuthResponse auth, String url) {
        ResponseEntity<OutboxEventPageResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authorizedEntity(auth),
                OutboxEventPageResponse.class
        );
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isNotNull();
        return response.getBody();
    }

    private NotificationPageResponse fetchPassengerNotifications(AuthResponse auth, String url) {
        ResponseEntity<NotificationPageResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authorizedEntity(auth),
                NotificationPageResponse.class
        );
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isNotNull();
        return response.getBody();
    }

    private AdminGlobalSearchResponse search(AuthResponse auth, String url, Object... uriVariables) {
        ResponseEntity<AdminGlobalSearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                authorizedEntity(auth),
                AdminGlobalSearchResponse.class,
                uriVariables
        );
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getGroups()).isNotNull();
        return response.getBody();
    }

    private SearchResultGroupResponse group(AdminGlobalSearchResponse response, String type) {
        return response.getGroups().stream()
                .filter(group -> type.equals(group.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected search group " + type));
    }

    private RiskEventResponse createPendingRisk(long userId, TrainSearchResponse train, String passengerPrefix) {
        createPaidOrder(userId, train, passengerPrefix + "A");
        createPaidOrder(userId, train, passengerPrefix + "B");
        createPaidOrder(userId, train, passengerPrefix + "C");
        return fetchRisks("/api/risks?status=PENDING&userId={userId}&size=20", userId).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected pending risk event for user " + userId));
    }

    private long countRisksForUser(long userId) {
        List<RiskEventResponse> risks = fetchRisks("/api/risks?userId={userId}&size=100", userId);
        return risks.stream()
                .filter(risk -> Long.valueOf(userId).equals(risk.getUserId()))
                .count();
    }

    private ResponseEntity<String> createOrderRaw(long userId, Long trainId, Long inventoryId, String passengerName) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setTrainId(trainId);
        request.setInventoryId(inventoryId);
        request.setPassengerName(passengerName);
        request.setPassengerIdCard("11010120000202" + userId);
        return restTemplate.postForEntity("/api/orders", request, String.class);
    }

    private OrderResponse createPassengerOrder(AuthResponse auth, TrainSearchResponse train, String passengerName) {
        PassengerCreateOrderRequest request = new PassengerCreateOrderRequest();
        request.setTrainId(train.getTrainId());
        request.setInventoryId(train.getInventoryId());
        request.setPassengerName(passengerName);
        request.setPassengerIdCard("32010119900101" + String.format("%04d", Math.abs(passengerName.hashCode()) % 10000));
        request.setRequestId("passenger-order-" + auth.getUsername() + "-" + System.nanoTime());
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/api/passenger/orders",
                HttpMethod.POST,
                authorizedEntity(auth, request),
                OrderResponse.class
        );
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private int userGetStatus(AuthResponse auth, String path) {
        ResponseEntity<String> response = restTemplate.exchange(
                path,
                HttpMethod.GET,
                authorizedEntity(auth),
                String.class
        );
        return response.getStatusCodeValue();
    }

    private SeatInventory createSingleSeatInventory() {
        String suffix = Long.toString(System.nanoTime(), 36);
        if (suffix.length() > 8) {
            suffix = suffix.substring(suffix.length() - 8);
        }
        Station departure = stationRepository.save(new Station("CF" + suffix, "并发测试始发站", "测试"));
        Station arrival = stationRepository.save(new Station("CT" + suffix, "并发测试到达站", "测试"));
        Train train = trainRepository.save(new Train(
                "C" + suffix,
                departure,
                arrival,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        ));
        return seatInventoryRepository.save(new SeatInventory(
                train,
                LocalDate.now().plusDays(30),
                "SECOND_CLASS",
                1,
                new BigDecimal("88.00")
        ));
    }

    private TrainSearchResponse createTicketChangeInventory(BigDecimal price, String label) {
        String suffix = Long.toString(System.nanoTime(), 36);
        if (suffix.length() > 7) {
            suffix = suffix.substring(suffix.length() - 7);
        }
        Station departure = stationRepository.save(new Station("TCF" + suffix + label.charAt(0), "改签测试始发" + label, "测试"));
        Station arrival = stationRepository.save(new Station("TCT" + suffix + label.charAt(0), "改签测试到达" + label, "测试"));
        Train train = trainRepository.save(new Train(
                "TC" + label + suffix,
                departure,
                arrival,
                LocalTime.of("OLD".equals(label) ? 8 : 10, 0),
                LocalTime.of("OLD".equals(label) ? 9 : 11, 20)
        ));
        SeatInventory inventory = seatInventoryRepository.save(new SeatInventory(
                train,
                LocalDate.now().plusDays(10),
                "SECOND_CLASS",
                8,
                price
        ));
        return TrainSearchResponse.from(inventory);
    }

    private AuthResponse login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return restTemplate.postForObject("/api/auth/login", request, AuthResponse.class);
    }

    private int postLoginStatus(String username, String password) throws Exception {
        URL url = new URL("http://localhost:" + port + "/api/auth/login");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bytes);
        }
        return connection.getResponseCode();
    }

    private int postWithoutTokenStatus(String path) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    private RiskEventResponse handleRisk(Long riskId, AuthResponse auth) {
        return handleRisk(riskId, auth, "CLOSED", "旧版标记已处理兼容调用");
    }

    private RiskEventResponse handleRisk(Long riskId, AuthResponse auth, String status, String remark) {
        ResponseEntity<RiskEventResponse> response = restTemplate.exchange(
                "/api/risks/{id}/handle",
                HttpMethod.POST,
                authorizedEntity(auth, riskHandleRequest(status, remark)),
                RiskEventResponse.class,
                riskId
        );
        return response.getBody();
    }

    private ResponseEntity<String> handleRiskRaw(Long riskId, AuthResponse auth, String status, String remark) {
        return restTemplate.exchange(
                "/api/risks/{id}/handle",
                HttpMethod.POST,
                authorizedEntity(auth, riskHandleRequest(status, remark)),
                String.class,
                riskId
        );
    }

    private RiskHandleRequest riskHandleRequest(String status, String remark) {
        RiskHandleRequest request = new RiskHandleRequest();
        request.setStatus(status);
        request.setRemark(remark);
        return request;
    }

    private List<RiskEventResponse> fetchRisks(String url, Object... uriVariables) {
        return fetchRiskPage(url, uriVariables).getContent();
    }

    private RiskEventPageResponse fetchRiskPage(String url, Object... uriVariables) {
        RiskEventPageResponse response = restTemplate.getForObject(url, RiskEventPageResponse.class, uriVariables);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        return response;
    }

    private List<RiskEventHandleRecordResponse> fetchRiskHandleRecords(Long riskId) {
        RiskEventHandleRecordResponse[] records = restTemplate.getForObject(
                "/api/risks/{id}/handle-records",
                RiskEventHandleRecordResponse[].class,
                riskId
        );
        assertThat(records).isNotNull();
        return Arrays.asList(records);
    }

    private OperationLog[] latestLogs(AuthResponse auth) {
        ResponseEntity<OperationLog[]> response = restTemplate.exchange(
                "/api/logs",
                HttpMethod.GET,
                authorizedEntity(auth),
                OperationLog[].class
        );
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private TrainSearchCacheStats trainSearchCacheStats(AuthResponse auth) {
        ResponseEntity<TrainSearchCacheStats> response = restTemplate.exchange(
                "/api/cache/train-search",
                HttpMethod.GET,
                authorizedEntity(auth),
                TrainSearchCacheStats.class
        );
        return response.getBody();
    }

    private RateLimitSummary rateLimitSummary(AuthResponse auth) {
        ResponseEntity<RateLimitSummary> response = restTemplate.exchange(
                "/api/rate-limit/summary",
                HttpMethod.GET,
                authorizedEntity(auth),
                RateLimitSummary.class
        );
        return response.getBody();
    }

    private void clearTrainSearchCache(AuthResponse auth) {
        restTemplate.exchange(
                "/api/cache/train-search",
                HttpMethod.DELETE,
                authorizedEntity(auth),
                TrainSearchCacheStats.class
        );
    }

    private HttpEntity<Void> authorizedEntity(AuthResponse auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + auth.getToken());
        return new HttpEntity<Void>(headers);
    }

    private HttpEntity<Void> authorizedEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<Void>(headers);
    }

    private <T> HttpEntity<T> authorizedEntity(AuthResponse auth, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + auth.getToken());
        return new HttpEntity<T>(body, headers);
    }

    private TrainSearchResponse firstTrainInventory() {
        return searchToday().stream()
                .filter(train -> "SECOND_CLASS".equals(train.getSeatType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected train inventory"));
    }

    private TrainSearchResponse findInventory(Long inventoryId) {
        return searchToday().stream()
                .filter(train -> inventoryId.equals(train.getInventoryId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected inventory " + inventoryId));
    }

    private List<TrainSearchResponse> searchToday() {
        String date = LocalDate.now().toString();
        TrainSearchResponse[] trains = restTemplate.getForObject(
                "/api/trains/search?from=BJP&to=SHH&date={date}",
                TrainSearchResponse[].class,
                date
        );
        assertThat(trains).isNotNull();
        return Arrays.asList(trains);
    }
}
