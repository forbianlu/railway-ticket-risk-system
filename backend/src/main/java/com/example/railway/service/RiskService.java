package com.example.railway.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.RiskEventHandleRecord;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.RiskStatus;
import com.example.railway.domain.TicketOrder;
import com.example.railway.dto.RiskEventHandleRecordResponse;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.dto.RiskHandleRequest;
import com.example.railway.repository.RiskEventHandleRecordRepository;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.service.risk.RiskContext;
import com.example.railway.service.risk.RiskHit;
import com.example.railway.service.risk.RiskRule;

@Service
public class RiskService {

    private final RiskEventRepository riskEventRepository;
    private final RiskEventHandleRecordRepository handleRecordRepository;
    private final OperationLogService operationLogService;
    private final List<RiskRule> riskRules;

    public RiskService(RiskEventRepository riskEventRepository,
                       RiskEventHandleRecordRepository handleRecordRepository,
                       OperationLogService operationLogService,
                       List<RiskRule> riskRules) {
        this.riskEventRepository = riskEventRepository;
        this.handleRecordRepository = handleRecordRepository;
        this.operationLogService = operationLogService;
        this.riskRules = riskRules;
    }

    @Transactional
    public void evaluateAfterOrderCreated(TicketOrder order) {
        evaluate(order, RiskScene.ORDER_CREATED);
    }

    @Transactional
    public void evaluateAfterRefund(TicketOrder order) {
        evaluate(order, RiskScene.ORDER_REFUNDED);
    }

    @Transactional(readOnly = true)
    public List<RiskEventResponse> latestRisks() {
        return latestRisks(null, null);
    }

    @Transactional(readOnly = true)
    public List<RiskEventResponse> latestRisks(String statusValue, String sceneValue) {
        RiskStatus status = parseStatus(statusValue, false);
        RiskScene scene = parseScene(sceneValue, false);
        List<RiskEvent> riskEvents;
        if (status != null && scene != null) {
            riskEvents = riskEventRepository.findTop50ByStatusAndSceneOrderByCreatedAtDesc(status, scene);
        } else if (status != null) {
            riskEvents = riskEventRepository.findTop50ByStatusOrderByCreatedAtDesc(status);
        } else if (scene != null) {
            riskEvents = riskEventRepository.findTop50BySceneOrderByCreatedAtDesc(scene);
        } else {
            riskEvents = riskEventRepository.findTop50ByOrderByCreatedAtDesc();
        }
        List<RiskEventResponse> responses = new ArrayList<RiskEventResponse>();
        for (RiskEvent riskEvent : riskEvents) {
            responses.add(RiskEventResponse.from(riskEvent));
        }
        return responses;
    }

    @Transactional
    public RiskEventResponse handleRisk(Long riskId, String operator) {
        return handleRisk(riskId, null, operator);
    }

    @Transactional
    public RiskEventResponse handleRisk(Long riskId, RiskHandleRequest request, String operator) {
        RiskEvent riskEvent = riskEventRepository.findById(riskId)
                .orElseThrow(() -> new BusinessException("风险事件不存在"));
        RiskStatus fromStatus = currentStatus(riskEvent);
        RiskStatus toStatus = parseTargetStatus(request);
        validateTransition(fromStatus, toStatus);

        LocalDateTime now = LocalDateTime.now();
        String currentOperator = normalizeOperator(operator);
        String remark = request == null ? null : normalizeText(request.getRemark());

        riskEvent.setStatus(toStatus);
        riskEvent.setHandled(!RiskStatus.PENDING.equals(toStatus));
        riskEvent.setHandleRemark(remark);
        riskEvent.setHandledBy(currentOperator);
        riskEvent.setHandledAt(now);
        if (RiskStatus.CLOSED.equals(toStatus)) {
            riskEvent.setClosedAt(now);
        }
        RiskEvent saved = riskEventRepository.save(riskEvent);

        RiskEventHandleRecord record = new RiskEventHandleRecord();
        record.setRiskEventId(saved.getId());
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setRemark(remark);
        record.setOperatorName(currentOperator);
        record.setOperatedAt(now);
        handleRecordRepository.save(record);

        operationLogService.record(
                currentOperator,
                "HANDLE_RISK_EVENT",
                "RISK_EVENT",
                String.valueOf(saved.getId()),
                buildHandleLogDetail(fromStatus, toStatus, remark)
        );
        return RiskEventResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RiskEventHandleRecordResponse> handleRecords(Long riskId) {
        if (!riskEventRepository.existsById(riskId)) {
            throw new BusinessException("风险事件不存在");
        }
        List<RiskEventHandleRecord> records = handleRecordRepository.findByRiskEventIdOrderByOperatedAtAsc(riskId);
        List<RiskEventHandleRecordResponse> responses = new ArrayList<RiskEventHandleRecordResponse>();
        for (RiskEventHandleRecord record : records) {
            responses.add(RiskEventHandleRecordResponse.from(record));
        }
        return responses;
    }

    private void evaluate(TicketOrder order, RiskScene scene) {
        RiskContext context = new RiskContext(order, LocalDateTime.now());
        for (RiskRule rule : riskRules) {
            if (!scene.equals(rule.scene())) {
                continue;
            }
            Optional<RiskHit> hit = rule.evaluate(context);
            if (hit.isPresent()) {
                createRiskEvent(order, scene, hit.get());
            }
        }
    }

    private void createRiskEvent(TicketOrder order, RiskScene scene, RiskHit hit) {
        RiskEvent riskEvent = new RiskEvent();
        riskEvent.setOrder(order);
        riskEvent.setUserId(order.getUserId());
        riskEvent.setRiskType(hit.getRiskType());
        riskEvent.setRiskLevel(hit.getRiskLevel());
        riskEvent.setScene(scene);
        riskEvent.setStatus(RiskStatus.PENDING);
        riskEvent.setReason(hit.getReason());
        riskEvent.setHandled(false);
        riskEvent.setCreatedAt(LocalDateTime.now());
        RiskEvent saved = riskEventRepository.save(riskEvent);
        operationLogService.record(
                "RISK_ENGINE",
                "CREATE_RISK_EVENT",
                "RISK_EVENT",
                String.valueOf(saved.getId()),
                hit.getReason()
        );
    }

    private RiskStatus parseTargetStatus(RiskHandleRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            return RiskStatus.CLOSED;
        }
        return parseStatus(request.getStatus(), true);
    }

    private RiskStatus parseStatus(String value, boolean required) {
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                throw new BusinessException("风险状态不能为空");
            }
            return null;
        }
        try {
            return RiskStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("风险状态不合法");
        }
    }

    private RiskScene parseScene(String value, boolean required) {
        if (value == null || value.trim().isEmpty()) {
            if (required) {
                throw new BusinessException("风险场景不能为空");
            }
            return null;
        }
        try {
            return RiskScene.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("风险场景不合法");
        }
    }

    private RiskStatus currentStatus(RiskEvent riskEvent) {
        if (riskEvent.getStatus() != null) {
            return riskEvent.getStatus();
        }
        return Boolean.TRUE.equals(riskEvent.getHandled()) ? RiskStatus.CLOSED : RiskStatus.PENDING;
    }

    private void validateTransition(RiskStatus fromStatus, RiskStatus toStatus) {
        if (RiskStatus.CLOSED.equals(fromStatus)) {
            throw new BusinessException("风险事件已关闭，不能重复处置");
        }
        if (RiskStatus.PENDING.equals(toStatus)) {
            throw new BusinessException("不能将风险事件处置为待处理");
        }
        if (RiskStatus.PENDING.equals(fromStatus)
                && (RiskStatus.CONFIRMED.equals(toStatus)
                || RiskStatus.FALSE_POSITIVE.equals(toStatus)
                || RiskStatus.CLOSED.equals(toStatus))) {
            return;
        }
        if ((RiskStatus.CONFIRMED.equals(fromStatus) || RiskStatus.FALSE_POSITIVE.equals(fromStatus))
                && RiskStatus.CLOSED.equals(toStatus)) {
            return;
        }
        throw new BusinessException("风险状态流转不合法");
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            return "RISK_OFFICER";
        }
        return operator.trim();
    }

    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }

    private String buildHandleLogDetail(RiskStatus fromStatus, RiskStatus toStatus, String remark) {
        String detail = "风险事件从 " + fromStatus.name() + " 处置为 " + toStatus.name();
        if (remark != null) {
            detail = detail + "，备注：" + remark;
        }
        if (detail.length() > 500) {
            return detail.substring(0, 500);
        }
        return detail;
    }
}
