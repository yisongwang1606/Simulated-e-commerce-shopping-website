package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.PaymentStatus;
import com.eason.ecom.messaging.OrderEventType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class CommerceMetricsServiceTest {

    @Test
    void incrementsBusinessAndKafkaCounters() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CommerceMetricsService commerceMetricsService = new CommerceMetricsService(meterRegistry);

        commerceMetricsService.incrementOrderCreated();
        commerceMetricsService.incrementOrderStatusTransition(OrderStatus.CREATED, OrderStatus.PAID);
        commerceMetricsService.incrementPaymentCallback(PaymentStatus.SUCCEEDED);
        commerceMetricsService.incrementKafkaPublished(OrderEventType.ORDER_CREATED);
        commerceMetricsService.incrementKafkaConsumed(OrderEventType.ORDER_CREATED, "stored");

        assertEquals(1.0, meterRegistry.get("ecom.orders.created.events").counter().count());
        assertEquals(1.0, meterRegistry.get("ecom.orders.status.transitions")
                .tags("from", "CREATED", "to", "PAID")
                .counter()
                .count());
        assertEquals(1.0, meterRegistry.get("ecom.payments.callback.events")
                .tags("status", "SUCCEEDED")
                .counter()
                .count());
        assertEquals(1.0, meterRegistry.get("ecom.kafka.order.events.published")
                .tags("eventType", "ORDER_CREATED")
                .counter()
                .count());
        assertEquals(1.0, meterRegistry.get("ecom.kafka.order.events.consumed")
                .tags("eventType", "ORDER_CREATED", "outcome", "stored")
                .counter()
                .count());
    }
}
