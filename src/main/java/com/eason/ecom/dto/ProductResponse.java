package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String category,
        String description,
        LocalDateTime createdAt) {
}
