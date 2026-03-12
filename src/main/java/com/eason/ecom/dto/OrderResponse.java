package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderResponse", description = "Order details with items, status, and totals")
public record OrderResponse(
        Long id,
        Long userId,
        String username,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
}
