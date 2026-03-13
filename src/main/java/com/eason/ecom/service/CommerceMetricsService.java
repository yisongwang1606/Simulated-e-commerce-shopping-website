package com.eason.ecom.service;

import org.springframework.stereotype.Service;

import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.PaymentStatus;
import com.eason.ecom.messaging.OrderEventType;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class CommerceMetricsService {

    private final MeterRegistry meterRegistry;

    public CommerceMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementOrderCreated() {
        meterRegistry.counter("ecom.orders.created.events").increment();
    }

    public void incrementOrderStatusTransition(OrderStatus fromStatus, OrderStatus toStatus) {
        meterRegistry.counter(
                "ecom.orders.status.transitions",
                "from", fromStatus.name(),
                "to", toStatus.name()).increment();
    }

    public void incrementPaymentInitiated(String paymentMethod) {
        meterRegistry.counter("ecom.payments.initiated.events", "method", paymentMethod).increment();
    }

    public void incrementPaymentCallback(PaymentStatus paymentStatus) {
        meterRegistry.counter("ecom.payments.callback.events", "status", paymentStatus.name()).increment();
    }

    public void incrementShipmentCreated(String carrierCode) {
        meterRegistry.counter("ecom.shipments.created.events", "carrier", carrierCode).increment();
    }

    public void incrementShipmentDelivered() {
        meterRegistry.counter("ecom.shipments.delivered.events").increment();
    }

    public void incrementRefundRequested() {
        meterRegistry.counter("ecom.refunds.requested.events").increment();
    }

    public void incrementRefundReviewed(String decision) {
        meterRegistry.counter("ecom.refunds.reviewed.events", "decision", decision).increment();
    }

    public void incrementRefundSettled() {
        meterRegistry.counter("ecom.refunds.settled.events").increment();
    }

    public void incrementKafkaPublished(OrderEventType eventType) {
        meterRegistry.counter("ecom.kafka.order.events.published", "eventType", eventType.name()).increment();
    }

    public void incrementKafkaPublishFailure(OrderEventType eventType) {
        meterRegistry.counter("ecom.kafka.order.events.publish.failures", "eventType", eventType.name()).increment();
    }

    public void incrementKafkaConsumed(OrderEventType eventType, String outcome) {
        meterRegistry.counter(
                "ecom.kafka.order.events.consumed",
                "eventType", eventType.name(),
                "outcome", outcome).increment();
    }

    public void incrementKafkaRetry(OrderEventType eventType) {
        meterRegistry.counter("ecom.kafka.order.events.retries", "eventType", eventType.name()).increment();
    }

    public void incrementKafkaDeadLetter(OrderEventType eventType) {
        meterRegistry.counter("ecom.kafka.order.events.dead.lettered", "eventType", eventType.name()).increment();
    }

}
