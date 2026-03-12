package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductResponse", description = "Product details returned by the API")
public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String category,
        String description,
        LocalDateTime createdAt) {
}
