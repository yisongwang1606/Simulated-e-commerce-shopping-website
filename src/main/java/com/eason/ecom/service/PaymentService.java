package com.eason.ecom.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.PaymentCallbackRequest;
import com.eason.ecom.dto.PaymentInitiationRequest;
import com.eason.ecom.dto.PaymentTransactionResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.PaymentMethod;
import com.eason.ecom.entity.PaymentStatus;
import com.eason.ecom.entity.PaymentTransaction;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.integration.stripe.StripePaymentGateway;
import com.eason.ecom.integration.stripe.StripePaymentIntentResult;
import com.eason.ecom.integration.stripe.StripeWebhookResult;
import com.eason.ecom.messaging.OrderEventType;
import com.eason.ecom.messaging.OrderLifecycleEventFactory;
import com.eason.ecom.messaging.OrderLifecycleEventPublisher;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.PaymentTransactionRepository;
import com.eason.ecom.support.PaymentReferenceGenerator;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderService orderService;
    private final RefundService refundService;
    private final AuditLogService auditLogService;
    private final CommerceMetricsService commerceMetricsService;
    private final PaymentReferenceGenerator paymentReferenceGenerator;
    private final OrderLifecycleEventFactory orderLifecycleEventFactory;
    private final OrderLifecycleEventPublisher orderLifecycleEventPublisher;
    private final StripePaymentGateway stripePaymentGateway;

    public PaymentService(
            PaymentTransactionRepository paymentTransactionRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderService orderService,
            RefundService refundService,
            AuditLogService auditLogService,
            CommerceMetricsService commerceMetricsService,
            PaymentReferenceGenerator paymentReferenceGenerator,
            OrderLifecycleEventFactory orderLifecycleEventFactory,
            OrderLifecycleEventPublisher orderLifecycleEventPublisher,
            StripePaymentGateway stripePaymentGateway) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderService = orderService;
        this.refundService = refundService;
        this.auditLogService = auditLogService;
        this.commerceMetricsService = commerceMetricsService;
        this.paymentReferenceGenerator = paymentReferenceGenerator;
        this.orderLifecycleEventFactory = orderLifecycleEventFactory;
        this.orderLifecycleEventPublisher = orderLifecycleEventPublisher;
        this.stripePaymentGateway = stripePaymentGateway;
    }

    @Transactional
    public PaymentTransactionResponse initiatePayment(
            Long orderId,
            PaymentInitiationRequest request,
            Long actorUserId,
            String actorUsername) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validatePaymentCreation(customerOrder);

        if (paymentTransactionRepository.existsByOrderIdAndPaymentStatus(orderId, PaymentStatus.SUCCEEDED)) {
            throw new BadRequestException("Order already has a successful payment");
        }

        if (customerOrder.getStatus() == OrderStatus.CREATED) {
            orderService.updateOrderStatusForAdmin(
                    orderId,
                    OrderStatus.PAYMENT_PENDING.name(),
                    "Payment initiated for order " + customerOrder.getOrderNo(),
                    actorUserId,
                    actorUsername);
        }

        String providerCode = resolveProviderCode(request.providerCode());
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setOrder(customerOrder);
        paymentTransaction.setPaymentMethod(resolvePaymentMethod(request.paymentMethod()));
        paymentTransaction.setPaymentStatus(PaymentStatus.PENDING);
        paymentTransaction.setTransactionRef(paymentReferenceGenerator.next());
        paymentTransaction.setProviderCode(providerCode);
        paymentTransaction.setAmount(request.amount());
        paymentTransaction.setNote(normalizeOptional(request.note()));

        StripePaymentIntentResult stripePaymentIntentResult = null;
        if ("STRIPE".equals(providerCode)) {
            stripePaymentIntentResult = stripePaymentGateway.createPaymentIntent(customerOrder, paymentTransaction, request);
            paymentTransaction.setProviderReference(stripePaymentIntentResult.providerReference());
            paymentTransaction.setNote(resolveProviderNote(paymentTransaction.getNote(), stripePaymentIntentResult.note()));
        }

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(paymentTransaction);
        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "PAYMENT_INITIATED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Payment initiated for order " + customerOrder.getOrderNo(),
                Map.of(
                        "paymentId", savedTransaction.getId(),
                        "transactionRef", savedTransaction.getTransactionRef(),
                        "providerReference", savedTransaction.getProviderReference() == null
                                ? ""
                                : savedTransaction.getProviderReference(),
                        "paymentMethod", savedTransaction.getPaymentMethod().name(),
                        "amount", savedTransaction.getAmount()));
        commerceMetricsService.incrementPaymentInitiated(savedTransaction.getPaymentMethod().name());
        orderLifecycleEventPublisher.publish(orderLifecycleEventFactory.create(
                OrderEventType.PAYMENT_INITIATED,
                customerOrder,
                "admin-payment-initiation",
                actorUsername,
                Map.of(
                        "paymentId", savedTransaction.getId(),
                        "transactionRef", savedTransaction.getTransactionRef(),
                        "providerReference", savedTransaction.getProviderReference() == null
                                ? ""
                                : savedTransaction.getProviderReference(),
                        "paymentMethod", savedTransaction.getPaymentMethod().name(),
                        "amount", savedTransaction.getAmount(),
                        "paymentStatus", savedTransaction.getPaymentStatus().name())));

        PaymentTransactionResponse response = toResponse(
                savedTransaction,
                stripePaymentIntentResult == null ? null : stripePaymentIntentResult.clientSecret());
        if (stripePaymentIntentResult != null
                && isTerminalProviderStatus(stripePaymentIntentResult.paymentStatus())) {
            PaymentTransactionResponse callbackResponse = handleCallback(new PaymentCallbackRequest(
                    savedTransaction.getTransactionRef(),
                    stripePaymentIntentResult.paymentStatus(),
                    stripePaymentIntentResult.providerReference(),
                    savedTransaction.getProviderCode(),
                    stripePaymentIntentResult.note()));
            return withClientSecret(callbackResponse, stripePaymentIntentResult.clientSecret());
        }
        return response;
    }

    @Transactional
    public PaymentTransactionResponse handleCallback(PaymentCallbackRequest request) {
        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByTransactionRef(request.transactionRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        PaymentStatus targetStatus = resolvePaymentStatus(request.paymentStatus());
        String normalizedProviderEventId = normalizeOptional(request.providerEventId());
        String normalizedProviderCode = resolveProviderCode(request.providerCode());
        String normalizedNote = normalizeOptional(request.note());
        if (isIdempotentRepeat(paymentTransaction, targetStatus, normalizedProviderEventId)) {
            return toResponse(paymentTransaction, null);
        }

        paymentTransaction.setPaymentStatus(targetStatus);
        paymentTransaction.setProviderCode(normalizedProviderCode);
        paymentTransaction.setProviderEventId(normalizedProviderEventId);
        paymentTransaction.setNote(normalizedNote);

        CustomerOrder customerOrder = paymentTransaction.getOrder();
        if (targetStatus == PaymentStatus.SUCCEEDED) {
            paymentTransaction.setPaidAt(LocalDateTime.now());
            moveOrderToPaidIfNeeded(customerOrder);
        } else if (targetStatus == PaymentStatus.REFUNDED) {
            moveOrderToRefundedIfNeeded(customerOrder);
            refundService.markSettledForOrder(customerOrder);
        }

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(paymentTransaction);
        auditLogService.recordUserAction(
                null,
                "payment-callback",
                "PAYMENT_CALLBACK_PROCESSED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Payment callback processed for order " + customerOrder.getOrderNo(),
                Map.of(
                        "paymentId", savedTransaction.getId(),
                        "transactionRef", savedTransaction.getTransactionRef(),
                        "paymentStatus", savedTransaction.getPaymentStatus().name()));
        commerceMetricsService.incrementPaymentCallback(savedTransaction.getPaymentStatus());
        orderLifecycleEventPublisher.publish(orderLifecycleEventFactory.create(
                OrderEventType.PAYMENT_UPDATED,
                customerOrder,
                "payment-callback",
                "payment-callback",
                Map.of(
                        "paymentId", savedTransaction.getId(),
                        "transactionRef", savedTransaction.getTransactionRef(),
                        "paymentStatus", savedTransaction.getPaymentStatus().name(),
                        "providerEventId", savedTransaction.getProviderEventId() == null
                                ? ""
                                : savedTransaction.getProviderEventId())));

        return toResponse(savedTransaction, null);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getPaymentsForOrder(Long orderId) {
        return paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(transaction -> toResponse(transaction, null))
                .toList();
    }

    @Transactional
    public String handleStripeWebhook(String payload, String signatureHeader) {
        StripeWebhookResult webhookResult = stripePaymentGateway.parseWebhook(payload, signatureHeader);
        if (!webhookResult.isHandled()) {
            return "Ignored Stripe event " + webhookResult.eventType();
        }
        PaymentTransactionResponse response = handleCallback(webhookResult.callbackRequest());
        return "Processed Stripe event " + webhookResult.eventType()
                + " for transaction " + response.transactionRef();
    }

    private void validatePaymentCreation(CustomerOrder customerOrder) {
        if (customerOrder.getStatus() != OrderStatus.CREATED
                && customerOrder.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BadRequestException("Payment cannot be initiated for the current order status");
        }
    }

    private void moveOrderToPaidIfNeeded(CustomerOrder customerOrder) {
        if (customerOrder.getStatus() == OrderStatus.CREATED) {
            orderService.updateOrderStatusForAdmin(
                    customerOrder.getId(),
                    OrderStatus.PAYMENT_PENDING.name(),
                    "Payment callback marked the order as pending settlement",
                    null,
                    "payment-callback");
        }
        if (customerOrder.getStatus() == OrderStatus.PAYMENT_PENDING) {
            orderService.updateOrderStatusForAdmin(
                    customerOrder.getId(),
                    OrderStatus.PAID.name(),
                    "Payment callback confirmed settlement",
                    null,
                    "payment-callback");
        }
    }

    private void moveOrderToRefundedIfNeeded(CustomerOrder customerOrder) {
        if (customerOrder.getStatus() == OrderStatus.SHIPPED) {
            orderService.updateOrderStatusForAdmin(
                    customerOrder.getId(),
                    OrderStatus.REFUND_PENDING.name(),
                    "Refund callback received for shipped order",
                    null,
                    "payment-callback");
        }
        if (customerOrder.getStatus() == OrderStatus.REFUND_PENDING) {
            orderService.updateOrderStatusForAdmin(
                    customerOrder.getId(),
                    OrderStatus.REFUNDED.name(),
                    "Payment callback confirmed refund settlement",
                    null,
                    "payment-callback");
        }
    }

    private PaymentMethod resolvePaymentMethod(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(paymentMethod.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Unsupported payment method: " + paymentMethod);
        }
    }

    private PaymentStatus resolvePaymentStatus(String paymentStatus) {
        try {
            return PaymentStatus.valueOf(paymentStatus.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Unsupported payment status: " + paymentStatus);
        }
    }

    private boolean isIdempotentRepeat(
            PaymentTransaction paymentTransaction,
            PaymentStatus targetStatus,
            String normalizedProviderEventId) {
        if (paymentTransaction.getPaymentStatus() != targetStatus) {
            return false;
        }
        if (StringUtils.hasText(normalizedProviderEventId)
                && normalizedProviderEventId.equals(paymentTransaction.getProviderEventId())) {
            return true;
        }
        return targetStatus == PaymentStatus.SUCCEEDED && paymentTransaction.getPaidAt() != null;
    }

    private String resolveProviderCode(String providerCode) {
        return StringUtils.hasText(providerCode) ? providerCode.trim().toUpperCase() : "SIMULATED_GATEWAY";
    }

    private boolean isTerminalProviderStatus(String paymentStatus) {
        return "SUCCEEDED".equals(paymentStatus)
                || "FAILED".equals(paymentStatus)
                || "CANCELLED".equals(paymentStatus);
    }

    private String resolveProviderNote(String requestedNote, String providerNote) {
        if (!StringUtils.hasText(providerNote)) {
            return requestedNote;
        }
        if (!StringUtils.hasText(requestedNote)) {
            return providerNote;
        }
        return requestedNote + " | " + providerNote;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private PaymentTransactionResponse toResponse(
            PaymentTransaction paymentTransaction,
            String clientSecret) {
        return new PaymentTransactionResponse(
                paymentTransaction.getId(),
                paymentTransaction.getOrder().getId(),
                paymentTransaction.getOrder().getOrderNo(),
                paymentTransaction.getPaymentMethod().name(),
                paymentTransaction.getPaymentStatus().name(),
                paymentTransaction.getTransactionRef(),
                paymentTransaction.getProviderCode(),
                paymentTransaction.getProviderEventId(),
                paymentTransaction.getProviderReference(),
                paymentTransaction.getAmount(),
                clientSecret,
                paymentTransaction.getNote(),
                paymentTransaction.getCreatedAt(),
                paymentTransaction.getPaidAt(),
                paymentTransaction.getUpdatedAt());
    }

    private PaymentTransactionResponse withClientSecret(
            PaymentTransactionResponse response,
            String clientSecret) {
        if (!StringUtils.hasText(clientSecret)) {
            return response;
        }
        return new PaymentTransactionResponse(
                response.id(),
                response.orderId(),
                response.orderNo(),
                response.paymentMethod(),
                response.paymentStatus(),
                response.transactionRef(),
                response.providerCode(),
                response.providerEventId(),
                response.providerReference(),
                response.amount(),
                clientSecret,
                response.note(),
                response.createdAt(),
                response.paidAt(),
                response.updatedAt());
    }
}
