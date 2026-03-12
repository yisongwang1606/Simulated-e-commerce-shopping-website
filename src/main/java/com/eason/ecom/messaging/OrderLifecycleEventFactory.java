package com.eason.ecom.messaging;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eason.ecom.entity.CustomerOrder;

@Component
public class OrderLifecycleEventFactory {

    public OrderLifecycleEvent create(
            OrderEventType eventType,
            CustomerOrder customerOrder,
            String source,
            String actorUsername,
            Map<String, ?> payload) {
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        if (payload != null) {
            eventPayload.putAll(payload);
        }
        return new OrderLifecycleEvent(
                UUID.randomUUID().toString(),
                eventType,
                customerOrder.getId(),
                customerOrder.getOrderNo(),
                customerOrder.getStatus().name(),
                StringUtils.hasText(source) ? source.trim() : "system",
                StringUtils.hasText(actorUsername) ? actorUsername.trim() : "system",
                Instant.now(),
                Collections.unmodifiableMap(eventPayload));
    }
}
