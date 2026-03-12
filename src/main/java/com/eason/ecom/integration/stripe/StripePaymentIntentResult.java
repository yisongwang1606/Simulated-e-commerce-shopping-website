package com.eason.ecom.integration.stripe;

public record StripePaymentIntentResult(
        String providerReference,
        String clientSecret,
        String paymentStatus,
        String note) {
}
