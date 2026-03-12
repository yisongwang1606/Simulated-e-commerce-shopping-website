package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuditLogResponse", description = "Operational audit trail event")
public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        String actionType,
        String entityType,
        String entityId,
        String summary,
        String detailsJson,
        LocalDateTime createdAt) {
}
