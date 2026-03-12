package com.eason.ecom.integration.stripe;

import com.eason.ecom.dto.PaymentCallbackRequest;

public record StripeWebhookResult(
        String eventType,
        PaymentCallbackRequest callbackRequest) {

    public boolean isHandled() {
        return callbackRequest != null;
    }
}
