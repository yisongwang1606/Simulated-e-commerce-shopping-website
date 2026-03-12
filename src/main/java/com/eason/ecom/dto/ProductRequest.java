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
        @Schema(description = "Optional enterprise SKU", example = "ELE-0001")
        @Size(max = 32) String sku,
        @Schema(description = "Display name of the product", example = "Gaming Keyboard")
        @NotBlank @Size(max = 120) String name,
        @Schema(description = "Brand or supplier-facing label", example = "NorthPeak Tech")
        @Size(max = 80) String brand,
        @Schema(description = "Unit price", example = "89.99")
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @Schema(description = "Optional internal cost price", example = "54.20")
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal costPrice,
        @Schema(description = "Available stock quantity", example = "25")
        @NotNull @PositiveOrZero Integer stock,
        @Schema(description = "Safety stock threshold", example = "5")
        @PositiveOrZero Integer safetyStock,
        @Schema(description = "Category used for filtering", example = "Electronics")
        @NotBlank @Size(max = 50) String category,
        @Schema(description = "Public product lifecycle status", example = "ACTIVE")
        String status,
        @Schema(description = "Tax class used by order calculations", example = "STANDARD")
        String taxClass,
        @Schema(description = "Shipping weight in kilograms", example = "0.86")
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal weightKg,
        @Schema(description = "Lead time in days", example = "7")
        @PositiveOrZero Integer leadTimeDays,
        @Schema(description = "Whether the product is highlighted in merchandising surfaces", example = "true")
        Boolean featured,
        @Schema(description = "Long-form product description", example = "Mechanical keyboard for backend API testing.")
        @NotBlank @Size(max = 4000) String description) {
}
