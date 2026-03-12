package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "OrderTagAssignmentRequest", description = "Assign an operational tag to an order")
public record OrderTagAssignmentRequest(
        @Schema(description = "Tag identifier from the order tag catalog", example = "1")
        @NotNull(message = "orderTagId must not be null")
        @Positive(message = "orderTagId must be positive")
        Long orderTagId) {
}
