package com.eason.ecom.integration.stripe;

import com.eason.ecom.dto.PaymentInitiationRequest;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.PaymentTransaction;

public interface StripePaymentGateway {

    boolean isConfigured();

    StripePaymentIntentResult createPaymentIntent(
            CustomerOrder customerOrder,
            PaymentTransaction paymentTransaction,
            PaymentInitiationRequest request);

    StripeWebhookResult parseWebhook(String payload, String signatureHeader);
}
