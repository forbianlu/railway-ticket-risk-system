package com.example.railway.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.OperationLog;
import com.example.railway.repository.OperationLogRepository;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional
    public void record(String operator, String action, String targetType, String targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setOperator(operator);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<OperationLog> latest() {
        return operationLogRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
