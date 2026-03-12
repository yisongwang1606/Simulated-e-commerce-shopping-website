package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.entity.OrderEventReceipt;
import com.eason.ecom.messaging.OrderEventType;
import com.eason.ecom.messaging.OrderLifecycleEvent;
import com.eason.ecom.repository.OrderEventReceiptRepository;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderEventReceiptServiceTest {

    @Mock
    private OrderEventReceiptRepository orderEventReceiptRepository;

    private OrderEventReceiptService orderEventReceiptService;

    @BeforeEach
    void setUp() {
        orderEventReceiptService = new OrderEventReceiptService(
                orderEventReceiptRepository,
                new AppProperties(),
                new ObjectMapper());
    }

    @Test
    void recordsNewEventReceipt() {
        OrderLifecycleEvent event = new OrderLifecycleEvent(
                "evt-1",
                OrderEventType.ORDER_CREATED,
                55L,
                "ORD-202603120001-1001",
                "CREATED",
                "customer-checkout",
                "demo",
                Instant.parse("2026-03-12T22:00:00Z"),
                Map.of("itemCount", 2));
        when(orderEventReceiptRepository.existsByEventIdAndConsumerGroup("evt-1", "ecom-order-events"))
                .thenReturn(false);

        boolean recorded = orderEventReceiptService.record(event);

        assertTrue(recorded);
        ArgumentCaptor<OrderEventReceipt> captor = ArgumentCaptor.forClass(OrderEventReceipt.class);
        verify(orderEventReceiptRepository).save(captor.capture());
        assertEquals("evt-1", captor.getValue().getEventId());
        assertEquals("ORDER_CREATED", captor.getValue().getEventType());
        assertEquals("ORD-202603120001-1001", captor.getValue().getOrderNo());
        assertEquals("ecom-order-events", captor.getValue().getConsumerGroup());
    }

    @Test
    void skipsDuplicateEventForSameConsumerGroup() {
        OrderLifecycleEvent event = new OrderLifecycleEvent(
                "evt-1",
                OrderEventType.ORDER_CREATED,
                55L,
                "ORD-202603120001-1001",
                "CREATED",
                "customer-checkout",
                "demo",
                Instant.parse("2026-03-12T22:00:00Z"),
                Map.of());
        when(orderEventReceiptRepository.existsByEventIdAndConsumerGroup("evt-1", "ecom-order-events"))
                .thenReturn(true);

        boolean recorded = orderEventReceiptService.record(event);

        assertFalse(recorded);
        verify(orderEventReceiptRepository, never()).save(any(OrderEventReceipt.class));
    }
}
