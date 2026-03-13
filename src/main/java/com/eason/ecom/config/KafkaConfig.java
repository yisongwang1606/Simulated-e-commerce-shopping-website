package com.eason.ecom.config;

import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.eason.ecom.messaging.OrderLifecycleEvent;
import com.eason.ecom.service.CommerceMetricsService;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    }

    @Bean
    public NewTopic orderLifecycleTopic(AppProperties appProperties) {
        return TopicBuilder.name(appProperties.getKafka().getOrderTopic())
                .partitions(appProperties.getKafka().getTopicPartitions())
                .replicas(appProperties.getKafka().getTopicReplicas())
                .build();
    }

    @Bean
    public NewTopic orderLifecycleDeadLetterTopic(AppProperties appProperties) {
        return TopicBuilder.name(appProperties.getKafka().getDeadLetterTopic())
                .partitions(appProperties.getKafka().getTopicPartitions())
                .replicas(appProperties.getKafka().getTopicReplicas())
                .build();
    }

    @Bean
    public ProducerFactory<String, OrderLifecycleEvent> orderEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        properties.put("spring.json.add.type.headers", false);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, OrderLifecycleEvent> orderEventKafkaTemplate(
            ProducerFactory<String, OrderLifecycleEvent> orderEventProducerFactory) {
        return new KafkaTemplate<>(orderEventProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, OrderLifecycleEvent> orderEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.consumer-group}") String consumerGroup) {
        Map<String, Object> properties = new java.util.HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put("spring.json.trusted.packages", "com.eason.ecom.messaging");
        properties.put("spring.json.value.default.type", OrderLifecycleEvent.class.getName());
        properties.put("spring.json.use.type.headers", false);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderLifecycleEvent> orderEventKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderLifecycleEvent> orderEventConsumerFactory,
            CommonErrorHandler orderEventKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, OrderLifecycleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderEventConsumerFactory);
        factory.setCommonErrorHandler(orderEventKafkaErrorHandler);
        return factory;
    }

    @Bean
    public CommonErrorHandler orderEventKafkaErrorHandler(
            KafkaTemplate<String, OrderLifecycleEvent> orderEventKafkaTemplate,
            AppProperties appProperties,
            CommerceMetricsService commerceMetricsService) {
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer =
                new DeadLetterPublishingRecoverer(
                        orderEventKafkaTemplate,
                        (record, exception) -> new TopicPartition(
                                appProperties.getKafka().getDeadLetterTopic(),
                                record.partition()));
        ConsumerRecordRecoverer consumerRecordRecoverer = (record, exception) -> {
            OrderLifecycleEvent event = record.value() instanceof OrderLifecycleEvent payload ? payload : null;
            if (event != null && event.eventType() != null) {
                commerceMetricsService.incrementKafkaDeadLetter(event.eventType());
            }
            deadLetterPublishingRecoverer.accept(record, null, exception);
        };

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                consumerRecordRecoverer,
                new FixedBackOff(
                        appProperties.getKafka().getConsumerRetryBackoffMs(),
                        appProperties.getKafka().getConsumerMaxRetries()));
        errorHandler.setRetryListeners(new RetryListener() {
            @Override
            public void failedDelivery(
                    org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record,
                    Exception exception,
                    int deliveryAttempt) {
                if (record.value() instanceof OrderLifecycleEvent event && event.eventType() != null) {
                    commerceMetricsService.incrementKafkaRetry(event.eventType());
                }
            }
        });
        return errorHandler;
    }
}
