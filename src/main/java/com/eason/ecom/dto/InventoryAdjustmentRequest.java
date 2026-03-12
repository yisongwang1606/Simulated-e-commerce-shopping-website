package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "InventoryAdjustmentRequest", description = "Manual stock adjustment payload used by warehouse or catalog administrators")
public record InventoryAdjustmentRequest(
        @Schema(description = "Adjustment mode. Supported values: INCREASE, DECREASE, CORRECTION", example = "INCREASE")
        @NotBlank(message = "adjustmentType must not be blank")
        String adjustmentType,

        @Schema(description = "Signed quantity delta. Use positive for increase, negative for correction down, positive for decrease with DECREASE type", example = "12")
        @NotNull(message = "quantityDelta must not be null")
        Integer quantityDelta,

        @Schema(description = "Business reason for the stock change", example = "Cycle count variance resolved by warehouse audit")
        @NotBlank(message = "reason must not be blank")
        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason,

        @Schema(description = "Reference entity type such as PURCHASE_ORDER or STOCKTAKE", example = "STOCKTAKE")
        @Size(max = 40, message = "referenceType must be at most 40 characters")
        String referenceType,

        @Schema(description = "Reference number from the upstream process", example = "ST-20260312-01")
        @Size(max = 64, message = "referenceId must be at most 64 characters")
        String referenceId) {
}
