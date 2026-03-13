package com.eason.ecom.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitConfig {

    @Bean
    public TopicExchange orderLifecycleExchange(AppProperties appProperties) {
        return new TopicExchange(appProperties.getRabbitmq().getExchange(), true, false);
    }

    @Bean
    public Queue orderLifecycleQueue(AppProperties appProperties) {
        return QueueBuilder.durable(appProperties.getRabbitmq().getOrderQueue()).build();
    }

    @Bean
    public Binding orderLifecycleBinding(
            TopicExchange orderLifecycleExchange,
            Queue orderLifecycleQueue,
            AppProperties appProperties) {
        return BindingBuilder.bind(orderLifecycleQueue)
                .to(orderLifecycleExchange)
                .with(appProperties.getRabbitmq().getRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter orderEventRabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter orderEventRabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(orderEventRabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory orderEventRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter orderEventRabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(orderEventRabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
