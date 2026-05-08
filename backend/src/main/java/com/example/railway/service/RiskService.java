package com.example.railway.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.common.BusinessException;
import com.example.railway.domain.RiskEvent;
import com.example.railway.domain.RiskScene;
import com.example.railway.domain.TicketOrder;
import com.example.railway.dto.RiskEventResponse;
import com.example.railway.repository.RiskEventRepository;
import com.example.railway.service.risk.RiskContext;
import com.example.railway.service.risk.RiskHit;
import com.example.railway.service.risk.RiskRule;

@Service
public class RiskService {

    private final RiskEventRepository riskEventRepository;
    private final OperationLogService operationLogService;
    private final List<RiskRule> riskRules;

    public RiskService(RiskEventRepository riskEventRepository,
                       OperationLogService operationLogService,
                       List<RiskRule> riskRules) {
        this.riskEventRepository = riskEventRepository;
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
        List<RiskEvent> riskEvents = riskEventRepository.findTop50ByOrderByCreatedAtDesc();
        List<RiskEventResponse> responses = new ArrayList<RiskEventResponse>();
        for (RiskEvent riskEvent : riskEvents) {
            responses.add(RiskEventResponse.from(riskEvent));
        }
        return responses;
    }

    @Transactional
    public RiskEventResponse handleRisk(Long riskId, String operator) {
        RiskEvent riskEvent = riskEventRepository.findById(riskId)
                .orElseThrow(() -> new BusinessException("风险事件不存在"));
        riskEvent.setHandled(true);
        RiskEvent saved = riskEventRepository.save(riskEvent);
        operationLogService.record(
                operator == null || operator.trim().isEmpty() ? "RISK_OFFICER" : operator,
                "HANDLE_RISK_EVENT",
                "RISK_EVENT",
                String.valueOf(saved.getId()),
                "风险事件已处理"
        );
        return RiskEventResponse.from(saved);
    }

    private void evaluate(TicketOrder order, RiskScene scene) {
        RiskContext context = new RiskContext(order, LocalDateTime.now());
        for (RiskRule rule : riskRules) {
            if (!scene.equals(rule.scene())) {
                continue;
            }
            Optional<RiskHit> hit = rule.evaluate(context);
            if (hit.isPresent()) {
                createRiskEvent(order, hit.get());
            }
        }
    }

    private void createRiskEvent(TicketOrder order, RiskHit hit) {
        RiskEvent riskEvent = new RiskEvent();
        riskEvent.setOrder(order);
        riskEvent.setUserId(order.getUserId());
        riskEvent.setRiskType(hit.getRiskType());
        riskEvent.setRiskLevel(hit.getRiskLevel());
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
}
