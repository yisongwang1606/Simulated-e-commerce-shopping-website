package com.eason.ecom.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.service.CommerceMetricsService;

@Component
public class OrderLifecycleKafkaPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderLifecycleKafkaPublisher.class);

    private final KafkaTemplate<String, OrderLifecycleEvent> kafkaTemplate;
    private final AppProperties appProperties;
    private final CommerceMetricsService commerceMetricsService;

    public OrderLifecycleKafkaPublisher(
            KafkaTemplate<String, OrderLifecycleEvent> kafkaTemplate,
            AppProperties appProperties,
            CommerceMetricsService commerceMetricsService) {
        this.kafkaTemplate = kafkaTemplate;
        this.appProperties = appProperties;
        this.commerceMetricsService = commerceMetricsService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(OrderLifecycleEvent event) {
        kafkaTemplate.send(appProperties.getKafka().getOrderTopic(), event.orderNo(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        commerceMetricsService.incrementKafkaPublishFailure(event.eventType());
                        LOGGER.warn("Failed to publish order event {} for {}", event.eventType(), event.orderNo(), error);
                        return;
                    }
                    commerceMetricsService.incrementKafkaPublished(event.eventType());
                });
    }
}
