package com.eason.ecom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartItemRequest", description = "Payload for adding a product to the cart")
public record CartItemRequest(
        @Schema(description = "Product identifier", example = "5")
        @NotNull @Positive Long productId,
        @Schema(description = "Quantity to add", example = "2")
        @NotNull @Positive Integer quantity) {
}
