package com.eason.ecom.messaging;

import java.time.Instant;
import java.util.Map;

public record OrderLifecycleEvent(
        String eventId,
        OrderEventType eventType,
        Long orderId,
        String orderNo,
        String orderStatus,
        String source,
        String actorUsername,
        Instant occurredAt,
        Map<String, Object> payload) {
}
