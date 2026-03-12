package com.eason.ecom.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrderLifecycleEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderLifecycleEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(OrderLifecycleEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
