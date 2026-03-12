package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "SupportTicketCreateRequest", description = "Customer support ticket payload")
public record SupportTicketCreateRequest(
        @Schema(description = "Operational category", example = "DELIVERY")
        @NotBlank(message = "category must not be blank")
        @Size(max = 40, message = "category must be at most 40 characters")
        String category,

        @Schema(description = "Priority", example = "HIGH")
        @NotBlank(message = "priority must not be blank")
        @Size(max = 16, message = "priority must be at most 16 characters")
        String priority,

        @Schema(description = "Short subject line", example = "Parcel marked delivered but not received")
        @NotBlank(message = "subject must not be blank")
        @Size(max = 140, message = "subject must be at most 140 characters")
        String subject,

        @Schema(description = "Customer-provided issue description", example = "Tracking says delivered at 9:13 AM, but nothing reached my condo front desk.")
        @NotBlank(message = "customerMessage must not be blank")
        @Size(max = 4000, message = "customerMessage must be at most 4000 characters")
        String customerMessage) {
}
