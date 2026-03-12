package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "ShipmentStatusUpdateRequest", description = "Shipment delivery update payload")
public record ShipmentStatusUpdateRequest(
        @Schema(description = "Delivery note", example = "Carrier confirmed proof of delivery at front desk")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
