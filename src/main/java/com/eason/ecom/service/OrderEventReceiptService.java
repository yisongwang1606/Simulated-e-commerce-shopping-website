package com.eason.ecom.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.entity.OrderEventReceipt;
import com.eason.ecom.messaging.OrderLifecycleEvent;
import com.eason.ecom.repository.OrderEventReceiptRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class OrderEventReceiptService {

    private final OrderEventReceiptRepository orderEventReceiptRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public OrderEventReceiptService(
            OrderEventReceiptRepository orderEventReceiptRepository,
            AppProperties appProperties,
            ObjectMapper objectMapper) {
        this.orderEventReceiptRepository = orderEventReceiptRepository;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean record(OrderLifecycleEvent event) {
        String consumerGroup = appProperties.getKafka().getConsumerGroup();
        if (orderEventReceiptRepository.existsByEventIdAndConsumerGroup(event.eventId(), consumerGroup)) {
            return false;
        }

        OrderEventReceipt orderEventReceipt = new OrderEventReceipt();
        orderEventReceipt.setEventId(event.eventId());
        orderEventReceipt.setEventType(event.eventType().name());
        orderEventReceipt.setOrderId(event.orderId());
        orderEventReceipt.setOrderNo(event.orderNo());
        orderEventReceipt.setOrderStatus(event.orderStatus());
        orderEventReceipt.setConsumerGroup(consumerGroup);
        orderEventReceipt.setPublishedAt(event.occurredAt());
        orderEventReceipt.setPayloadJson(writePayload(event));
        orderEventReceiptRepository.save(orderEventReceipt);
        return true;
    }

    private String writePayload(OrderLifecycleEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ignored) {
            return null;
        }
    }
}
