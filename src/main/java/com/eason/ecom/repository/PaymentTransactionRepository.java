package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.PaymentStatus;
import com.eason.ecom.entity.PaymentTransaction;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<PaymentTransaction> findByOrderIdAndProviderReference(Long orderId, String providerReference);

    boolean existsByOrderIdAndPaymentStatus(Long orderId, PaymentStatus paymentStatus);
}
