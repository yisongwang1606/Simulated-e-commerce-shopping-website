package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "PaymentCallbackRequest", description = "Simulated payment gateway callback payload")
public record PaymentCallbackRequest(
        @Schema(description = "Merchant-side payment reference", example = "PAY-20260312143000123-4821")
        @NotBlank(message = "transactionRef must not be blank")
        String transactionRef,

        @Schema(description = "Callback payment status", example = "SUCCEEDED")
        @NotBlank(message = "paymentStatus must not be blank")
        String paymentStatus,

        @Schema(description = "Gateway event identifier", example = "evt_ca_20260312_001")
        @Size(max = 64, message = "providerEventId must be at most 64 characters")
        String providerEventId,

        @Schema(description = "Gateway code", example = "SIMULATED_GATEWAY")
        @Size(max = 40, message = "providerCode must be at most 40 characters")
        String providerCode,

        @Schema(description = "Operational note from callback processor", example = "Payment settled successfully in demo environment")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
