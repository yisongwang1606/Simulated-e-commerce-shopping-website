package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductResponse", description = "Product details returned by the API")
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String brand,
        BigDecimal price,
        Integer stock,
        Integer safetyStock,
        String category,
        String status,
        String taxClass,
        BigDecimal weightKg,
        Integer leadTimeDays,
        boolean featured,
        String description,
        LocalDateTime createdAt) {
}
