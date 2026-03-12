package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartResponse", description = "Shopping cart summary with items and totals")
public record CartResponse(
        List<CartItemResponse> items,
        int totalQuantity,
        BigDecimal totalPrice) {
}
