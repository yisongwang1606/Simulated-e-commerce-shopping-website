package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefundRequestResponse", description = "Refund request and review state")
public record RefundRequestResponse(
        Long id,
        Long orderId,
        String orderNo,
        String refundStatus,
        String reason,
        String reviewNote,
        Long requestedByUserId,
        String requestedByUsername,
        Long reviewedByUserId,
        String reviewedByUsername,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt) {
}
