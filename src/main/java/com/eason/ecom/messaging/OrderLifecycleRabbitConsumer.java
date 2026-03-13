package com.eason.ecom.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.service.CommerceMetricsService;
import com.eason.ecom.service.OrderEventReceiptService;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class OrderLifecycleRabbitConsumer {

    private final OrderEventReceiptService orderEventReceiptService;
    private final CommerceMetricsService commerceMetricsService;
    private final AppProperties appProperties;

    public OrderLifecycleRabbitConsumer(
            OrderEventReceiptService orderEventReceiptService,
            CommerceMetricsService commerceMetricsService,
            AppProperties appProperties) {
        this.orderEventReceiptService = orderEventReceiptService;
        this.commerceMetricsService = commerceMetricsService;
        this.appProperties = appProperties;
    }

    @RabbitListener(
            queues = "${app.rabbitmq.order-queue}",
            containerFactory = "orderEventRabbitListenerContainerFactory")
    public void consume(OrderLifecycleEvent event) {
        boolean stored = orderEventReceiptService.record(event, appProperties.getRabbitmq().getConsumerGroup());
        commerceMetricsService.incrementRabbitConsumed(event.eventType(), stored ? "stored" : "duplicate");
    }
}
