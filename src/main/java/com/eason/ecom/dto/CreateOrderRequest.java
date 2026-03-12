package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(name = "CreateOrderRequest", description = "Optional payload to create an order from the current cart with an explicit shipping address")
public record CreateOrderRequest(
        @Schema(description = "Optional address identifier to snapshot on the order", example = "1")
        @Positive(message = "addressId must be greater than 0")
        Long addressId) {
}
