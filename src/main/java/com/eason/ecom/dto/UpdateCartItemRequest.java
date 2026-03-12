package com.eason.ecom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateCartItemRequest", description = "Payload for replacing the quantity of an item in the cart")
public record UpdateCartItemRequest(
        @Schema(description = "New cart quantity", example = "3")
        @NotNull @Min(1) Integer quantity) {
}
