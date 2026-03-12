package com.eason.ecom.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        String category,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal) {
}
