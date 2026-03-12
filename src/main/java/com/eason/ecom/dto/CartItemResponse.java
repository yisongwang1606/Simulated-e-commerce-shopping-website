package com.eason.ecom.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartItemResponse", description = "Single item inside the shopping cart")
public record CartItemResponse(
        Long productId,
        String name,
        String category,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal) {
}
