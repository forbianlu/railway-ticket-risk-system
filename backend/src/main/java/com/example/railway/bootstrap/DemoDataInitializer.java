package com.example.railway.bootstrap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.AppUser;
import com.example.railway.domain.OperationLog;
import com.example.railway.domain.OrderStatus;
import com.example.railway.domain.OutboxEvent;
import com.example.railway.domain.OutboxEventStatus;
import com.example.railway.domain.PaymentRecord;
import com.example.railway.domain.PaymentStatus;
import com.example.railway.domain.RefundRecord;
import com.example.railway.domain.RefundStatus;
import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.RiskEventHandleRecord;
import com.example.railway.domain.RiskLevel;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskStatus;
import com.example.railway.domain.RiskType;
import com.example.railway.domain.SeatInventory;
import com.example.railway.domain.Station;
import com.example.railway.domain.TicketOrder;
import com.example.railway.domain.Train;
import com.example.railway.domain.UserRole;
import com.example.railway.repository.AppUserRepository;
import com.example.railway.repository.OperationLogRepository;
import com.example.railway.repository.OutboxEventRepository;
import com.example.railway.repository.PaymentRecordRepository;
import com.example.railway.repository.RefundRecordRepository;
import com.example.railway.repository.RiskEventHandleRecordRepository;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.repository.SeatInventoryRepository;
import com.example.railway.repository.StationRepository;
import com.example.railway.repository.TicketOrderRepository;
import com.example.railway.repository.TrainRepository;
import com.example.railway.service.PasswordService;

@Component
public class DemoDataInitializer implements CommandLineRunner {

    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final AppUserRepository appUserRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final RiskEventRepository riskEventRepository;
    private final RiskEventHandleRecordRepository riskEventHandleRecordRepository;
    private final OperationLogRepository operationLogRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordService passwordService;
    private final boolean demoDataEnabled;

    public DemoDataInitializer(StationRepository stationRepository,
                               TrainRepository trainRepository,
                               SeatInventoryRepository seatInventoryRepository,
                               AppUserRepository appUserRepository,
                               TicketOrderRepository ticketOrderRepository,
                               PaymentRecordRepository paymentRecordRepository,
                               RefundRecordRepository refundRecordRepository,
                               RiskEventRepository riskEventRepository,
                               RiskEventHandleRecordRepository riskEventHandleRecordRepository,
                               OperationLogRepository operationLogRepository,
                               OutboxEventRepository outboxEventRepository,
                               PasswordService passwordService,
                               @Value("${railway.demo-data.enabled:true}") boolean demoDataEnabled) {
        this.stationRepository = stationRepository;
        this.trainRepository = trainRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.appUserRepository = appUserRepository;
        this.ticketOrderRepository = ticketOrderRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.refundRecordRepository = refundRecordRepository;
        this.riskEventRepository = riskEventRepository;
        this.riskEventHandleRecordRepository = riskEventHandleRecordRepository;
        this.operationLogRepository = operationLogRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.passwordService = passwordService;
        this.demoDataEnabled = demoDataEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        if (!demoDataEnabled) {
            return;
        }
        Map<String, Station> stations = seedStations();
        seedTrains(stations);
        seedInventories();
        seedOrdersAndMoneyRecords();
        seedRiskEvents();
        seedOperationLogs();
        seedOutboxEvents();
    }

    private void seedUsers() {
        if (appUserRepository.count() > 0) {
            return;
        }
        appUserRepository.save(new AppUser("admin", passwordService.hash("admin123"), "系统管理员", UserRole.ADMIN));
        appUserRepository.save(new AppUser("risk", passwordService.hash("risk123"), "风控专员", UserRole.RISK_OFFICER));
        appUserRepository.save(new AppUser("ops", passwordService.hash("ops123"), "运营人员", UserRole.OPERATOR));
    }

    private Map<String, Station> seedStations() {
        String[][] stationRows = {
                {"BJP", "北京南", "北京"},
                {"SHH", "上海虹桥", "上海"},
                {"GZQ", "广州南", "广州"},
                {"IOQ", "深圳北", "深圳"},
                {"HZD", "杭州东", "杭州"},
                {"NJH", "南京南", "南京"},
                {"WHN", "武汉", "武汉"},
                {"XAY", "西安北", "西安"},
                {"CDD", "成都东", "成都"},
                {"CQW", "重庆北", "重庆"},
                {"ZZF", "郑州东", "郑州"},
                {"CSQ", "长沙南", "长沙"},
                {"HFG", "合肥南", "合肥"},
                {"TXP", "天津西", "天津"},
                {"JNK", "济南西", "济南"},
                {"NXG", "南昌西", "南昌"},
                {"FZS", "福州南", "福州"}
        };

        Map<String, Station> stations = new HashMap<String, Station>();
        for (String[] row : stationRows) {
            Station station = stationRepository.findByCode(row[0])
                    .orElseGet(() -> stationRepository.save(new Station(row[0], row[1], row[2])));
            stations.put(row[0], station);
        }
        return stations;
    }

    private void seedTrains(Map<String, Station> stations) {
        Object[][] trainRows = {
                {"G101", "BJP", "SHH", LocalTime.of(7, 0), LocalTime.of(12, 38)},
                {"G102", "SHH", "BJP", LocalTime.of(14, 0), LocalTime.of(19, 35)},
                {"G305", "BJP", "HZD", LocalTime.of(9, 15), LocalTime.of(14, 28)},
                {"G306", "HZD", "BJP", LocalTime.of(15, 12), LocalTime.of(20, 30)},
                {"G606", "GZQ", "WHN", LocalTime.of(8, 6), LocalTime.of(12, 20)},
                {"G707", "IOQ", "GZQ", LocalTime.of(10, 10), LocalTime.of(10, 48)},
                {"G808", "CDD", "CQW", LocalTime.of(11, 30), LocalTime.of(13, 2)},
                {"G909", "XAY", "ZZF", LocalTime.of(12, 5), LocalTime.of(14, 10)},
                {"D2201", "WHN", "CDD", LocalTime.of(7, 42), LocalTime.of(16, 5)},
                {"D3105", "SHH", "FZS", LocalTime.of(8, 12), LocalTime.of(14, 47)},
                {"G501", "BJP", "CSQ", LocalTime.of(7, 42), LocalTime.of(14, 38)},
                {"G812", "CSQ", "GZQ", LocalTime.of(16, 0), LocalTime.of(18, 36)},
                {"G1203", "TXP", "SHH", LocalTime.of(9, 45), LocalTime.of(15, 18)},
                {"G1501", "JNK", "NXG", LocalTime.of(10, 22), LocalTime.of(16, 55)},
                {"D931", "HFG", "SHH", LocalTime.of(13, 20), LocalTime.of(16, 12)},
                {"C201", "NJH", "HFG", LocalTime.of(17, 10), LocalTime.of(18, 28)}
        };

        for (Object[] row : trainRows) {
            String trainNo = (String) row[0];
            if (!trainRepository.findByTrainNo(trainNo).isPresent()) {
                trainRepository.save(new Train(
                        trainNo,
                        stations.get((String) row[1]),
                        stations.get((String) row[2]),
                        (LocalTime) row[3],
                        (LocalTime) row[4]
                ));
            }
        }
    }

    private void seedInventories() {
        List<Train> trains = trainRepository.findAll();
        trains.sort(Comparator.comparing(Train::getTrainNo));
        LocalDate today = LocalDate.now();
        for (int trainIndex = 0; trainIndex < trains.size(); trainIndex++) {
            Train train = trains.get(trainIndex);
            for (int day = 0; day < 14; day++) {
                LocalDate travelDate = today.plusDays(day);
                saveInventoryIfMissing(train, travelDate, "SECOND_CLASS", totalSeats(trainIndex, day, 160), price(trainIndex, "553.00"));
                if (!train.getTrainNo().startsWith("D") && !train.getTrainNo().startsWith("C")) {
                    saveInventoryIfMissing(train, travelDate, "FIRST_CLASS", totalSeats(trainIndex, day, 48), price(trainIndex, "933.00"));
                }
                if (trainIndex % 3 == 0) {
                    saveInventoryIfMissing(train, travelDate, "BUSINESS_CLASS", totalSeats(trainIndex, day, 18), price(trainIndex, "1800.00"));
                }
            }
        }
    }

    private void saveInventoryIfMissing(Train train, LocalDate travelDate, String seatType, int totalSeats, BigDecimal price) {
        if (seatInventoryRepository.findByTrain_TrainNoAndTravelDateAndSeatType(train.getTrainNo(), travelDate, seatType).isPresent()) {
            return;
        }
        SeatInventory inventory = new SeatInventory(train, travelDate, seatType, totalSeats, price);
        int remaining = totalSeats;
        if ((train.getId() + travelDate.getDayOfMonth() + seatType.length()) % 9 == 0) {
            remaining = Math.min(totalSeats, 3 + travelDate.getDayOfMonth() % 3);
        } else if ((train.getId() + travelDate.getDayOfMonth()) % 5 == 0) {
            remaining = Math.max(8, totalSeats - 12);
        }
        inventory.setRemainingSeats(remaining);
        seatInventoryRepository.save(inventory);
    }

    private int totalSeats(int trainIndex, int day, int base) {
        return base + (trainIndex % 5) * 12 + (day % 4) * 6;
    }

    private BigDecimal price(int trainIndex, String base) {
        return new BigDecimal(base).add(new BigDecimal(trainIndex * 23));
    }

    private void seedOrdersAndMoneyRecords() {
        if (ticketOrderRepository.count() > 0) {
            return;
        }

        List<SeatInventory> inventories = new ArrayList<SeatInventory>(seatInventoryRepository.findAll());
        inventories.sort(Comparator
                .comparing((SeatInventory inventory) -> inventory.getTrain().getTrainNo())
                .thenComparing(SeatInventory::getTravelDate)
                .thenComparing(SeatInventory::getSeatType));

        OrderStatus[] statuses = {
                OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.CLOSED, OrderStatus.REFUNDED
        };
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 48 && i < inventories.size(); i++) {
            SeatInventory inventory = inventories.get(i);
            OrderStatus status = statuses[i % statuses.length];
            TicketOrder order = new TicketOrder();
            order.setOrderNo(String.format("DEMO20260518%04d", i + 1));
            order.setUserId(10001L + i % 8);
            order.setRequestId("demo-order-request-" + (i + 1));
            order.setPassengerName("演示乘客" + String.format("%02d", i + 1));
            order.setPassengerIdCard("1101011990" + String.format("%08d", i + 1));
            order.setTrain(inventory.getTrain());
            order.setInventory(inventory);
            order.setTravelDate(inventory.getTravelDate());
            order.setSeatType(inventory.getSeatType());
            order.setAmount(inventory.getPrice());
            order.setStatus(status);
            order.setCreatedAt(now.minusDays(i % 18).minusHours(i % 6));
            order.setPaymentDeadlineAt(status == OrderStatus.PENDING_PAYMENT ? now.plusDays(2).plusMinutes(i) : order.getCreatedAt().plusMinutes(20));
            if (status == OrderStatus.PAID || status == OrderStatus.REFUNDED) {
                order.setPaidAt(order.getCreatedAt().plusMinutes(8));
            }
            if (status == OrderStatus.REFUNDED) {
                order.setRefundedAt(order.getCreatedAt().plusDays(1));
            }
            if (status == OrderStatus.CLOSED) {
                order.setClosedAt(order.getCreatedAt().plusMinutes(18));
            }
            if (status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.PAID) {
                inventory.deductOne();
                seatInventoryRepository.save(inventory);
            }
            TicketOrder savedOrder = ticketOrderRepository.save(order);
            seedPaymentRecord(savedOrder, i);
            if (status == OrderStatus.REFUNDED) {
                seedRefundRecord(savedOrder, i);
            }
        }
    }

    private void seedPaymentRecord(TicketOrder order, int index) {
        String paymentNo = String.format("PAYDEMO20260518%04d", index + 1);
        if (paymentRecordRepository.findByPaymentNo(paymentNo).isPresent()) {
            return;
        }
        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setUserId(order.getUserId());
        payment.setAmount(order.getAmount());
        payment.setChannel("MOCK");
        payment.setRequestId("demo-payment-request-" + (index + 1));
        payment.setCreatedAt(order.getCreatedAt().plusMinutes(1));
        payment.setUpdatedAt(order.getCreatedAt().plusMinutes(3));
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.REFUNDED) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCallbackRequestId("demo-payment-callback-" + (index + 1));
            payment.setCallbackMessage("mock payment success");
            payment.setChannelPaymentNo("CH_PAY_DEMO_" + String.format("%04d", index + 1));
            payment.setPaidAt(order.getPaidAt());
            payment.setUpdatedAt(order.getPaidAt());
        } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCallbackMessage("waiting for payment callback");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCallbackRequestId("demo-payment-failed-callback-" + (index + 1));
            payment.setCallbackMessage("mock payment failed");
        }
        paymentRecordRepository.save(payment);
    }

    private void seedRefundRecord(TicketOrder order, int index) {
        String refundNo = String.format("RFDEMO20260518%04d", index + 1);
        if (refundRecordRepository.findByRefundNo(refundNo).isPresent()) {
            return;
        }
        RefundStatus[] statuses = {RefundStatus.PENDING, RefundStatus.SUCCESS, RefundStatus.FAILED};
        RefundStatus status = statuses[(index / 4) % statuses.length];
        RefundRecord refund = new RefundRecord();
        refund.setRefundNo(refundNo);
        refund.setPaymentNo(String.format("PAYDEMO20260518%04d", index + 1));
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(order.getUserId());
        refund.setAmount(order.getAmount());
        refund.setStatus(status);
        refund.setChannel("MOCK");
        refund.setRequestId("demo-refund-request-" + (index + 1));
        refund.setCreatedAt(order.getRefundedAt().plusMinutes(2));
        refund.setUpdatedAt(order.getRefundedAt().plusMinutes(5));
        if (status == RefundStatus.SUCCESS) {
            refund.setCallbackRequestId("demo-refund-callback-" + (index + 1));
            refund.setCallbackMessage("mock refund success");
            refund.setChannelRefundNo("CH_RF_DEMO_" + String.format("%04d", index + 1));
            refund.setRefundedAt(order.getRefundedAt().plusMinutes(10));
            refund.setUpdatedAt(refund.getRefundedAt());
        } else if (status == RefundStatus.FAILED) {
            refund.setCallbackRequestId("demo-refund-failed-callback-" + (index + 1));
            refund.setCallbackMessage("mock refund failed");
        } else {
            refund.setCallbackMessage("waiting for refund callback");
        }
        refundRecordRepository.save(refund);
    }

    private void seedRiskEvents() {
        if (riskEventRepository.count() > 0) {
            return;
        }
        List<TicketOrder> orders = ticketOrderRepository.findAll();
        if (orders.isEmpty()) {
            return;
        }
        RiskStatus[] statuses = {RiskStatus.PENDING, RiskStatus.CONFIRMED, RiskStatus.FALSE_POSITIVE, RiskStatus.CLOSED};
        RiskType[] types = {RiskType.RAPID_PURCHASE, RiskType.HIGH_AMOUNT, RiskType.FREQUENT_REFUND};
        RiskLevel[] levels = {RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH};
        for (int i = 0; i < 32; i++) {
            TicketOrder order = orders.get(i % orders.size());
            RiskStatus status = statuses[i % statuses.length];
            RiskEvent event = new RiskEvent();
            event.setOrder(order);
            event.setUserId(order.getUserId());
            event.setRiskType(types[i % types.length]);
            event.setRiskLevel(levels[i % levels.length]);
            event.setScene(order.getStatus() == OrderStatus.REFUNDED ? RiskScene.ORDER_REFUNDED : RiskScene.ORDER_CREATED);
            event.setReason(riskReason(event.getRiskType(), order));
            event.setStatus(status);
            event.setCreatedAt(order.getCreatedAt().plusMinutes(12));
            if (status != RiskStatus.PENDING) {
                event.setHandled(true);
                event.setHandledBy(i % 2 == 0 ? "admin" : "risk");
                event.setHandledAt(event.getCreatedAt().plusHours(2));
                event.setHandleRemark(riskRemark(status));
                if (status == RiskStatus.CLOSED) {
                    event.setClosedAt(event.getHandledAt().plusHours(1));
                }
            }
            RiskEvent saved = riskEventRepository.save(event);
            seedRiskHandleRecords(saved);
        }
    }

    private String riskReason(RiskType riskType, TicketOrder order) {
        if (riskType == RiskType.RAPID_PURCHASE) {
            return "用户近期购票频次较高，订单号 " + order.getOrderNo();
        }
        if (riskType == RiskType.FREQUENT_REFUND) {
            return "用户近期退票行为较多，订单号 " + order.getOrderNo();
        }
        return "订单金额较高，需要关注支付与出票一致性，订单号 " + order.getOrderNo();
    }

    private String riskRemark(RiskStatus status) {
        if (status == RiskStatus.CONFIRMED) {
            return "已确认存在异常特征，继续纳入运营关注";
        }
        if (status == RiskStatus.FALSE_POSITIVE) {
            return "复核后判定为正常业务行为";
        }
        return "风险事件已完成复核并归档";
    }

    private void seedRiskHandleRecords(RiskEvent event) {
        if (event.getStatus() == RiskStatus.PENDING) {
            return;
        }
        saveRiskRecord(event.getId(), RiskStatus.PENDING,
                event.getStatus() == RiskStatus.CLOSED ? RiskStatus.CONFIRMED : event.getStatus(),
                event.getHandleRemark(),
                event.getHandledBy(),
                event.getHandledAt());
        if (event.getStatus() == RiskStatus.CLOSED) {
            saveRiskRecord(event.getId(), RiskStatus.CONFIRMED, RiskStatus.CLOSED,
                    "事件完成归档", event.getHandledBy(), event.getClosedAt());
        }
    }

    private void saveRiskRecord(Long riskEventId, RiskStatus fromStatus, RiskStatus toStatus,
                                String remark, String operatorName, LocalDateTime operatedAt) {
        RiskEventHandleRecord record = new RiskEventHandleRecord();
        record.setRiskEventId(riskEventId);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setRemark(remark);
        record.setOperatorName(operatorName);
        record.setOperatedAt(operatedAt);
        riskEventHandleRecordRepository.save(record);
    }

    private void seedOperationLogs() {
        if (operationLogRepository.count() > 0) {
            return;
        }
        String[] actions = {
                "LOGIN", "CREATE_ORDER", "PAY_ORDER", "CLOSE_ORDER", "REFUND_ORDER",
                "HANDLE_RISK_EVENT", "CREATE_PAYMENT", "PAYMENT_CALLBACK",
                "REFUND_CALLBACK", "OUTBOX_DISPATCH", "CACHE_CLEAR"
        };
        List<TicketOrder> orders = ticketOrderRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 66; i++) {
            String action = actions[i % actions.length];
            OperationLog log = new OperationLog();
            log.setOperator(i % 3 == 0 ? "admin" : (i % 3 == 1 ? "risk" : "ops"));
            log.setAction(action);
            log.setTargetType(action.contains("RISK") ? "RISK_EVENT" : (action.contains("CACHE") ? "CACHE" : "ORDER"));
            log.setTargetId(orders.isEmpty() ? String.valueOf(i + 1) : String.valueOf(orders.get(i % orders.size()).getId()));
            log.setDetail("演示数据操作日志：" + action);
            log.setCreatedAt(now.minusDays(i % 20).minusMinutes(i * 3L));
            operationLogRepository.save(log);
        }
    }

    private void seedOutboxEvents() {
        String[][] rows = {
                {"DEMO-OUTBOX-001", "ORDER_PAID", "ORDER", "1", "DONE"},
                {"DEMO-OUTBOX-002", "ORDER_REFUNDED", "ORDER", "2", "DONE"},
                {"DEMO-OUTBOX-003", "PAYMENT_SUCCEEDED", "PAYMENT", "3", "DONE"},
                {"DEMO-OUTBOX-004", "REFUND_CREATED", "REFUND", "4", "PENDING"},
                {"DEMO-OUTBOX-005", "REFUND_SUCCEEDED", "REFUND", "5", "PENDING"},
                {"DEMO-OUTBOX-006", "RISK_EVENT_HANDLED", "RISK", "6", "FAILED"},
                {"DEMO-OUTBOX-007", "PAYMENT_FAILED", "PAYMENT", "7", "FAILED"},
                {"DEMO-OUTBOX-008", "REFUND_FAILED", "REFUND", "8", "PROCESSING"},
                {"DEMO-OUTBOX-009", "RISK_EVENT_CREATED", "RISK", "9", "DONE"},
                {"DEMO-OUTBOX-010", "OUTBOX_DEMO_OBSERVE", "SYSTEM", "10", "PENDING"}
        };
        for (int i = 0; i < rows.length; i++) {
            OutboxEvent event = outboxEventRepository.findByEventId(rows[i][0]).orElseGet(OutboxEvent::new);
            event.setEventId(rows[i][0]);
            event.setEventType(rows[i][1]);
            event.setAggregateType(rows[i][2]);
            event.setAggregateId(rows[i][3]);
            event.setPayload("{\"source\":\"demo-data\",\"sequence\":" + (i + 1) + "}");
            event.setStatus(OutboxEventStatus.valueOf(rows[i][4]));
            event.setRetryCount(event.getStatus() == OutboxEventStatus.FAILED ? 3 : 0);
            event.setMaxRetryCount(3);
            event.setCreatedAt(LocalDateTime.now().minusHours(i + 1));
            event.setUpdatedAt(event.getCreatedAt().plusMinutes(10));
            event.setNextRetryAt(event.getStatus() == OutboxEventStatus.PENDING
                    ? LocalDateTime.now().plusDays(1)
                    : LocalDateTime.now().minusMinutes(5));
            if (event.getStatus() == OutboxEventStatus.DONE) {
                event.setProcessedAt(event.getUpdatedAt());
            }
            if (event.getStatus() == OutboxEventStatus.FAILED) {
                event.setLastError("demo handler failure for operations review");
            }
            outboxEventRepository.save(event);
        }
    }
}
