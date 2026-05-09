package com.example.railway;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Station;
import com.example.railway.domain.Train;
import com.example.railway.dto.AuthResponse;
import com.example.railway.dto.CreateOrderRequest;
import com.example.railway.dto.DashboardSummary;
import com.example.railway.dto.LoginRequest;
import com.example.railway.dto.OrderResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.TrainSearchCacheStats;
import com.example.railway.dto.TrainSearchResponse;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.StationRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.repository.TrainRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RailwayApiIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private SeatInventoryRepository seatInventoryRepository;

    @Autowired
    private TicketOrderRepository ticketOrderRepository;

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
    void shouldCacheTrainSearchAndEvictAfterInventoryChange() {
        AuthResponse admin = login("admin", "admin123");
        clearTrainSearchCache(admin);

        List<TrainSearchResponse> firstSearch = searchToday();
        List<TrainSearchResponse> secondSearch = searchToday();
        TrainSearchCacheStats afterSecondSearch = trainSearchCacheStats(admin);

        assertThat(firstSearch).isNotEmpty();
        assertThat(secondSearch).isNotEmpty();
        assertThat(afterSecondSearch.getEntryCount()).isGreaterThan(0);
        assertThat(afterSecondSearch.getHitCount()).isGreaterThan(0);

        createOrder(3106L, secondSearch.get(0), "CacheEvictUser");
        TrainSearchCacheStats afterOrder = trainSearchCacheStats(admin);

        assertThat(afterOrder.getEntryCount()).isEqualTo(0);
        assertThat(afterOrder.getEvictCount()).isGreaterThan(0);
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

    private TrainSearchCacheStats trainSearchCacheStats(AuthResponse auth) {
        ResponseEntity<TrainSearchCacheStats> response = restTemplate.exchange(
                "/api/cache/train-search",
                HttpMethod.GET,
                authorizedEntity(auth),
                TrainSearchCacheStats.class
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
