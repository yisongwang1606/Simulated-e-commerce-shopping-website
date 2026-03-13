package com.eason.ecom.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.OrderEventReceipt;

public interface OrderEventReceiptRepository extends JpaRepository<OrderEventReceipt, Long> {

    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    long countByOrderNo(String orderNo);

    long countByOrderNoAndConsumerGroup(String orderNo, String consumerGroup);
}
