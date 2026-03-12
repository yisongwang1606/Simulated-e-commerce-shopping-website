package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderResponse", description = "Order details with items, status, and totals")
public record OrderResponse(
        Long id,
        String orderNo,
        Long userId,
        String username,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal shippingAmount,
        BigDecimal discountAmount,
        BigDecimal totalPrice,
        OrderAddressSnapshotResponse shippingAddress,
        String status,
        String statusNote,
        LocalDateTime createdAt,
        LocalDateTime statusUpdatedAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items) {
}
