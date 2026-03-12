package com.eason.ecom.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderItemResponse", description = "Single line item stored in an order")
public record OrderItemResponse(
        Long productId,
        String sku,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal subtotal) {
}
