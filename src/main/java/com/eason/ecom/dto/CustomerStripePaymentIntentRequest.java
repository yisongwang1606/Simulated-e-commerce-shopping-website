package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "CustomerStripePaymentIntentRequest", description = "Customer checkout request that creates a Stripe PaymentIntent for the selected order")
public record CustomerStripePaymentIntentRequest(
        @Schema(description = "Optional customer note stored on the payment transaction", example = "Customer confirmed card payment at checkout")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
