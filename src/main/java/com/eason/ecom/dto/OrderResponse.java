package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String username,
        BigDecimal totalPrice,
        String status,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
}
