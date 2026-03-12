package com.eason.ecom.dto;

public record PopularProductResponse(
        ProductResponse product,
        double score) {
}
