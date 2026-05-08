package com.example.railway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.example.railway.dto.AuthResponse;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.DashboardSummary;
import com.example.railway.dto.LoginRequest;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.TrainSearchResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RailwayApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

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
        OrderResponse[] orders = restTemplate.getForObject("/api/orders?userId={userId}", OrderResponse[].class, userId);

        assertThat(created.getStatus()).isEqualTo("PAID");
        assertThat(orders).isNotNull();
        assertThat(Arrays.asList(orders))
                .extracting(OrderResponse::getOrderNo)
                .contains(created.getOrderNo());
    }

    @Test
    void shouldRefundOrderAndReleaseInventory() {
        TrainSearchResponse before = firstTrainInventory();
        long userId = 3102L;

        OrderResponse created = createOrder(userId, before, "RefundUser");
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
    void shouldGenerateAndHandleRiskEvents() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3103L;

        createOrder(userId, train, "RiskUserA");
        createOrder(userId, train, "RiskUserB");
        createOrder(userId, train, "RiskUserC");

        List<RiskEventResponse> risks = Arrays.asList(restTemplate.getForObject("/api/risks", RiskEventResponse[].class));
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
    void shouldProtectRiskHandlingWithRole() {
        TrainSearchResponse train = firstTrainInventory();
        long userId = 3105L;
        createOrder(userId, train, "ProtectedRiskA");
        createOrder(userId, train, "ProtectedRiskB");

        RiskEventResponse risk = Arrays.stream(restTemplate.getForObject("/api/risks", RiskEventResponse[].class))
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
    void dashboardShouldExposeOrderAndRiskMetrics() {
        TrainSearchResponse train = firstTrainInventory();
        createOrder(3104L, train, "DashboardUser");

        DashboardSummary summary = restTemplate.getForObject("/api/dashboard/summary", DashboardSummary.class);

        assertThat(summary.getTotalOrders()).isGreaterThan(0);
        assertThat(summary.getPaidOrders()).isGreaterThan(0);
        assertThat(summary.getPopularTrains()).isNotEmpty();
    }

    private OrderResponse createOrder(long userId, TrainSearchResponse train, String passengerName) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(userId);
        request.setTrainId(train.getTrainId());
        request.setInventoryId(train.getInventoryId());
        request.setPassengerName(passengerName);
        request.setPassengerIdCard("11010120000101" + userId);
        return restTemplate.postForObject("/api/orders", request, OrderResponse.class);
    }

    private AuthResponse login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return restTemplate.postForObject("/api/auth/login", request, AuthResponse.class);
    }

    private RiskEventResponse handleRisk(Long riskId, AuthResponse auth) {
        ResponseEntity<RiskEventResponse> response = restTemplate.exchange(
                "/api/risks/{id}/handle",
                HttpMethod.POST,
                authorizedEntity(auth),
                RiskEventResponse.class,
                riskId
        );
        return response.getBody();
    }

    private HttpEntity<Void> authorizedEntity(AuthResponse auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + auth.getToken());
        return new HttpEntity<Void>(headers);
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
