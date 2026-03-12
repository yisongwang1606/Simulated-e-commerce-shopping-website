package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PopularProductResponse", description = "Product with its popularity score from Redis ranking")
public record PopularProductResponse(
        ProductResponse product,
        double score) {
}
