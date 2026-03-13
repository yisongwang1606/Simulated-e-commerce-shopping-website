package com.eason.ecom.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.service.CommerceMetricsService;

@Component
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class OrderLifecycleRabbitPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderLifecycleRabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;
    private final CommerceMetricsService commerceMetricsService;

    public OrderLifecycleRabbitPublisher(
            RabbitTemplate rabbitTemplate,
            AppProperties appProperties,
            CommerceMetricsService commerceMetricsService) {
        this.rabbitTemplate = rabbitTemplate;
        this.appProperties = appProperties;
        this.commerceMetricsService = commerceMetricsService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(OrderLifecycleEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    appProperties.getRabbitmq().getExchange(),
                    appProperties.getRabbitmq().getRoutingKey(),
                    event);
            commerceMetricsService.incrementRabbitPublished(event.eventType());
        } catch (AmqpException exception) {
            commerceMetricsService.incrementRabbitPublishFailure(event.eventType());
            LOGGER.warn("Failed to publish RabbitMQ order event {} for {}", event.eventType(), event.orderNo(), exception);
        }
    }
}
