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

import com.example.railway.config.RateLimitProperties;
import com.example.railway.config.TrainSearchCacheProperties;
import com.example.railway.domain.OperationLog;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Station;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.Train;
import com.example.railway.dto.AuthResponse;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.CreatePaymentRequest;
import com.example.railway.dto.DashboardSummary;
import com.example.railway.dto.LoginRequest;
import com.example.railway.dto.OrderPageResponse;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.PaymentCallbackRequest;
import com.example.railway.dto.PaymentPageResponse;
import com.example.railway.dto.PaymentResponse;
import com.example.railway.dto.RateLimitSummary;
import com.example.railway.dto.RiskEventHandleRecordResponse;
import com.example.railway.dto.RiskEventPageResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.RiskHandleRequest;
import com.example.railway.dto.RiskSummaryResponse;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.repository.AppUserRepository;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.StationRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.repository.TrainRepository;

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
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Autowired
    private TrainSearchCacheProperties trainSearchCacheProperties;

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
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPaymentNo(paymentNo);
        request.setCallbackRequestId(callbackRequestId);
        request.setSuccess(success);
        request.setMessage(success ? "mock payment success" : "mock payment failed");
        return restTemplate.postForObject("/api/payments/callback", request, PaymentResponse.class);
    }

    private ResponseEntity<String> callbackPaymentRaw(String paymentNo, String callbackRequestId, boolean success) {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPaymentNo(paymentNo);
        request.setCallbackRequestId(callbackRequestId);
        request.setSuccess(success);
        request.setMessage(success ? "mock payment success" : "mock payment failed");
        return restTemplate.postForEntity("/api/payments/callback", request, String.class);
    }

    private PaymentPageResponse fetchPaymentPage(String url, Object... uriVariables) {
        PaymentPageResponse response = restTemplate.getForObject(url, PaymentPageResponse.class, uriVariables);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotNull();
        return response;
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
