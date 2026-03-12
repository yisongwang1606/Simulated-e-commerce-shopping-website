package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InventoryAdjustmentResponse", description = "Inventory movement record captured for traceability")
public record InventoryAdjustmentResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        String adjustmentType,
        Integer quantityDelta,
        Integer previousStock,
        Integer newStock,
        String reason,
        String referenceType,
        String referenceId,
        String actorUsername,
        LocalDateTime createdAt) {
}
