package com.eason.ecom.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.aopalliance.intercept.MethodInterceptor;

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
    public TopicExchange orderLifecycleDeadLetterExchange(AppProperties appProperties) {
        return new TopicExchange(appProperties.getRabbitmq().getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue orderLifecycleDeadLetterQueue(AppProperties appProperties) {
        return QueueBuilder.durable(appProperties.getRabbitmq().getDeadLetterQueue()).build();
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
    public Binding orderLifecycleDeadLetterBinding(
            TopicExchange orderLifecycleDeadLetterExchange,
            Queue orderLifecycleDeadLetterQueue,
            AppProperties appProperties) {
        return BindingBuilder.bind(orderLifecycleDeadLetterQueue)
                .to(orderLifecycleDeadLetterExchange)
                .with(appProperties.getRabbitmq().getDeadLetterRoutingKey());
    }

    @Bean
    public JacksonJsonMessageConverter orderEventRabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter orderEventRabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(orderEventRabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory orderEventRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            JacksonJsonMessageConverter orderEventRabbitMessageConverter,
            MethodInterceptor orderEventRabbitRetryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(orderEventRabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(orderEventRabbitRetryInterceptor);
        return factory;
    }

    @Bean
    public MethodInterceptor orderEventRabbitRetryInterceptor(
            RabbitTemplate rabbitTemplate,
            AppProperties appProperties) {
        RepublishMessageRecoverer recoverer = new RepublishMessageRecoverer(
                rabbitTemplate,
                appProperties.getRabbitmq().getDeadLetterExchange(),
                appProperties.getRabbitmq().getDeadLetterRoutingKey());

        return RetryInterceptorBuilder.stateless()
                .maxRetries(appProperties.getRabbitmq().getConsumerMaxAttempts() - 1)
                .backOffOptions(
                        appProperties.getRabbitmq().getRetryInitialIntervalMs(),
                        appProperties.getRabbitmq().getRetryMultiplier(),
                        appProperties.getRabbitmq().getRetryMaxIntervalMs())
                .recoverer(recoverer)
                .build();
    }
}
