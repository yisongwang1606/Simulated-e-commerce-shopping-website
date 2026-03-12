package com.eason.ecom.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "PaymentInitiationRequest", description = "Placeholder payment initiation request for simulated gateway integration")
public record PaymentInitiationRequest(
        @Schema(description = "Payment method", example = "CARD")
        @NotBlank(message = "paymentMethod must not be blank")
        String paymentMethod,

        @Schema(description = "Amount to request from the payment provider", example = "129.99")
        @NotNull(message = "amount must not be null")
        @Positive(message = "amount must be greater than 0")
        BigDecimal amount,

        @Schema(description = "Optional external provider code", example = "SIMULATED_GATEWAY")
        @Size(max = 40, message = "providerCode must be at most 40 characters")
        String providerCode,

        @Schema(description = "Optional human note", example = "Customer approved card payment on checkout page")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
