package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "StripePaymentReconcileRequest", description = "Request that reconciles one Stripe PaymentIntent back into the internal payment workflow")
public record StripePaymentReconcileRequest(
        @Schema(description = "Stripe PaymentIntent identifier returned by Stripe.js after confirmPayment", example = "pi_3TAIzl6sJR5QEaTk02ucuzsY")
        @NotBlank(message = "providerReference must not be blank")
        @Size(max = 128, message = "providerReference must be at most 128 characters")
        String providerReference) {
}
