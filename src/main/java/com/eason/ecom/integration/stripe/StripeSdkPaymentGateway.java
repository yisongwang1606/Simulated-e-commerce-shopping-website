package com.eason.ecom.integration.stripe;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.PaymentCallbackRequest;
import com.eason.ecom.dto.PaymentInitiationRequest;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.PaymentTransaction;
import com.eason.ecom.exception.BadRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class StripeSdkPaymentGateway implements StripePaymentGateway {

    private static final String STRIPE_PROVIDER_CODE = "STRIPE";

    private final AppProperties appProperties;

    public StripeSdkPaymentGateway(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public boolean isConfigured() {
        AppProperties.Stripe stripe = appProperties.getStripe();
        return stripe.isEnabled() && StringUtils.hasText(stripe.getSecretKey());
    }

    @Override
    public StripePaymentIntentResult createPaymentIntent(
            CustomerOrder customerOrder,
            PaymentTransaction paymentTransaction,
            PaymentInitiationRequest request) {
        ensureStripeConfigured();
        boolean confirmImmediately = Boolean.TRUE.equals(request.confirmImmediately());
        String paymentMethodToken = resolvePaymentMethodToken(request.providerPaymentMethodToken(), confirmImmediately);

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(request.amount()))
                .setCurrency(resolveCurrency(request.currency()))
                .setDescription("Enterprise checkout for order " + customerOrder.getOrderNo())
                .setReceiptEmail(customerOrder.getUser().getEmail())
                .putMetadata("transactionRef", paymentTransaction.getTransactionRef())
                .putMetadata("orderId", customerOrder.getId().toString())
                .putMetadata("orderNo", customerOrder.getOrderNo())
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .setEnabled(true)
                        .build());

        if (StringUtils.hasText(paymentMethodToken)) {
            paramsBuilder.setPaymentMethod(paymentMethodToken);
        }
        if (confirmImmediately) {
            paramsBuilder.setConfirm(true);
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.create(
                    paramsBuilder.build(),
                    RequestOptions.builder()
                            .setApiKey(appProperties.getStripe().getSecretKey())
                            .build());
            return new StripePaymentIntentResult(
                    paymentIntent.getId(),
                    paymentIntent.getClientSecret(),
                    resolvePaymentStatus(paymentIntent, confirmImmediately),
                    resolveIntentNote(paymentIntent));
        } catch (StripeException exception) {
            throw new BadRequestException("Stripe payment initiation failed: " + exception.getMessage());
        }
    }

    @Override
    public StripeWebhookResult parseWebhook(String payload, String signatureHeader) {
        ensureWebhookConfigured(signatureHeader);
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    signatureHeader,
                    appProperties.getStripe().getWebhookSecret());
            return switch (event.getType()) {
                case "payment_intent.succeeded" -> new StripeWebhookResult(
                        event.getType(),
                        buildCallbackRequest(event, "SUCCEEDED", "Stripe webhook confirmed payment"));
                case "payment_intent.payment_failed" -> new StripeWebhookResult(
                        event.getType(),
                        buildCallbackRequest(event, "FAILED", "Stripe webhook reported a failed payment"));
                case "payment_intent.canceled" -> new StripeWebhookResult(
                        event.getType(),
                        buildCallbackRequest(event, "CANCELLED", "Stripe webhook reported a cancelled payment"));
                default -> new StripeWebhookResult(event.getType(), null);
            };
        } catch (SignatureVerificationException exception) {
            throw new BadRequestException("Stripe webhook signature verification failed");
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Stripe webhook payload could not be processed");
        }
    }

    private PaymentCallbackRequest buildCallbackRequest(
            Event event,
            String paymentStatus,
            String defaultNote) {
        Object stripeObject = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new BadRequestException("Stripe webhook payload is not available"));
        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            throw new BadRequestException("Stripe webhook payload is not a payment intent");
        }

        String transactionRef = paymentIntent.getMetadata() == null
                ? null
                : paymentIntent.getMetadata().get("transactionRef");
        if (!StringUtils.hasText(transactionRef)) {
            throw new BadRequestException("Stripe webhook payload is missing transaction metadata");
        }

        return new PaymentCallbackRequest(
                transactionRef,
                paymentStatus,
                event.getId(),
                STRIPE_PROVIDER_CODE,
                defaultNote);
    }

    private String resolveCurrency(String requestedCurrency) {
        String configuredCurrency = StringUtils.hasText(requestedCurrency)
                ? requestedCurrency
                : appProperties.getStripe().getCurrency();
        return configuredCurrency.trim().toLowerCase();
    }

    private String resolvePaymentMethodToken(String providedToken, boolean confirmImmediately) {
        if (StringUtils.hasText(providedToken)) {
            return providedToken.trim();
        }
        if (confirmImmediately) {
            return appProperties.getStripe().getDefaultTestPaymentMethod();
        }
        return null;
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String resolvePaymentStatus(PaymentIntent paymentIntent, boolean confirmImmediately) {
        String intentStatus = paymentIntent.getStatus();
        if ("succeeded".equals(intentStatus)) {
            return "SUCCEEDED";
        }
        if ("canceled".equals(intentStatus)) {
            return "CANCELLED";
        }
        if (confirmImmediately && paymentIntent.getLastPaymentError() != null) {
            return "FAILED";
        }
        return "PENDING";
    }

    private String resolveIntentNote(PaymentIntent paymentIntent) {
        if (paymentIntent.getLastPaymentError() != null
                && StringUtils.hasText(paymentIntent.getLastPaymentError().getMessage())) {
            return "Stripe response: " + paymentIntent.getLastPaymentError().getMessage();
        }
        return "Stripe PaymentIntent status: " + paymentIntent.getStatus();
    }

    private void ensureStripeConfigured() {
        if (!isConfigured()) {
            throw new BadRequestException("Stripe test mode is not configured");
        }
    }

    private void ensureWebhookConfigured(String signatureHeader) {
        ensureStripeConfigured();
        if (!StringUtils.hasText(signatureHeader)) {
            throw new BadRequestException("Missing Stripe-Signature header");
        }
        if (!StringUtils.hasText(appProperties.getStripe().getWebhookSecret())) {
            throw new BadRequestException("Stripe webhook secret is not configured");
        }
    }
}
