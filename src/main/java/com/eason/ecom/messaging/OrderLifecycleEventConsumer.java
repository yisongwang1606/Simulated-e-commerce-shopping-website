package com.eason.ecom.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.eason.ecom.service.CommerceMetricsService;
import com.eason.ecom.service.OrderEventReceiptService;

@Component
public class OrderLifecycleEventConsumer {

    private final OrderEventReceiptService orderEventReceiptService;
    private final CommerceMetricsService commerceMetricsService;

    public OrderLifecycleEventConsumer(
            OrderEventReceiptService orderEventReceiptService,
            CommerceMetricsService commerceMetricsService) {
        this.orderEventReceiptService = orderEventReceiptService;
        this.commerceMetricsService = commerceMetricsService;
    }

    @KafkaListener(
            topics = "${app.kafka.order-topic}",
            groupId = "${app.kafka.consumer-group}",
            containerFactory = "orderEventKafkaListenerContainerFactory")
    public void consume(OrderLifecycleEvent event) {
        boolean stored = orderEventReceiptService.record(event);
        commerceMetricsService.incrementKafkaConsumed(event.eventType(), stored ? "stored" : "duplicate");
    }
}
