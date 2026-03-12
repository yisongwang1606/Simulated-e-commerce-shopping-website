package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "OrderStatusUpdateRequest", description = "Admin request to move an order into the next allowed lifecycle status")
public record OrderStatusUpdateRequest(
        @Schema(description = "Target order status", example = "PAID")
        @NotBlank(message = "status must not be blank")
        String status,

        @Schema(description = "Operational note for the transition", example = "Payment captured through PSP settlement batch 20260312-AM")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
