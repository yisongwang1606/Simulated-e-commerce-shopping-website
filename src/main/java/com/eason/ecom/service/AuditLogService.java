package com.eason.ecom.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.AuditLogResponse;
import com.eason.ecom.entity.AuditLog;
import com.eason.ecom.repository.AuditLogRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordUserAction(
            Long actorUserId,
            String actorUsername,
            String actionType,
            String entityType,
            String entityId,
            String summary,
            Map<String, ?> details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actorUserId);
        auditLog.setActorUsername(actorUsername);
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setSummary(summary);
        auditLog.setDetailsJson(writeDetails(details));
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditTrail(String entityType, String entityId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        List<AuditLog> auditLogs;
        if (StringUtils.hasText(entityType) && StringUtils.hasText(entityId)) {
            auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    entityType.trim().toUpperCase(),
                    entityId.trim(),
                    pageRequest);
        } else if (StringUtils.hasText(entityType)) {
            auditLogs = auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(
                    entityType.trim().toUpperCase(),
                    pageRequest);
        } else if (StringUtils.hasText(entityId)) {
            auditLogs = auditLogRepository.findByEntityIdOrderByCreatedAtDesc(entityId.trim(), pageRequest);
        } else {
            auditLogs = auditLogRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        return auditLogs.stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActorUserId(),
                        log.getActorUsername(),
                        log.getActionType(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getSummary(),
                        log.getDetailsJson(),
                        log.getCreatedAt()))
                .toList();
    }

    private String writeDetails(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception ignored) {
            return null;
        }
    }
}
