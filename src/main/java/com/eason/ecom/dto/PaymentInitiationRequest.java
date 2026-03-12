package com.eason.ecom.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "PaymentInitiationRequest", description = "Payment initiation request supporting simulated and Stripe test-mode providers")
public record PaymentInitiationRequest(
        @Schema(description = "Payment method", example = "CARD")
        @NotBlank(message = "paymentMethod must not be blank")
        String paymentMethod,

        @Schema(description = "Amount to request from the payment provider", example = "129.99")
        @NotNull(message = "amount must not be null")
        @Positive(message = "amount must be greater than 0")
        BigDecimal amount,

        @Schema(description = "Optional external provider code", example = "STRIPE")
        @Size(max = 40, message = "providerCode must be at most 40 characters")
        String providerCode,

        @Schema(description = "Optional provider currency code. Stripe expects a lowercase ISO currency.", example = "cad")
        @Size(max = 10, message = "currency must be at most 10 characters")
        String currency,

        @Schema(description = "Optional provider payment method token, for example Stripe test mode token pm_card_visa.", example = "pm_card_visa")
        @Size(max = 128, message = "providerPaymentMethodToken must be at most 128 characters")
        String providerPaymentMethodToken,

        @Schema(description = "When true, the provider call will attempt to confirm the payment immediately.", example = "true")
        Boolean confirmImmediately,

        @Schema(description = "Optional human note", example = "Customer approved card payment on checkout page")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
