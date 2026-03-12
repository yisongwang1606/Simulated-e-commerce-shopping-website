package com.eason.ecom.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductRequest", description = "Payload for creating or updating a product")
public record ProductRequest(
        @Schema(description = "Display name of the product", example = "Gaming Keyboard")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "Unit price", example = "89.99")
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @Schema(description = "Available stock quantity", example = "25")
        @NotNull @PositiveOrZero Integer stock,
        @Schema(description = "Category used for filtering", example = "Electronics")
        @NotBlank @Size(max = 50) String category,
        @Schema(description = "Long-form product description", example = "Mechanical keyboard for backend API testing.")
        @NotBlank @Size(max = 4000) String description) {
}
