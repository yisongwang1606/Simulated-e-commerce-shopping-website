package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderInternalNoteResponse", description = "Admin-only internal note attached to an order")
public record OrderInternalNoteResponse(
        Long id,
        Long orderId,
        String orderNo,
        String noteText,
        Long createdByUserId,
        String createdByUsername,
        LocalDateTime createdAt) {
}
