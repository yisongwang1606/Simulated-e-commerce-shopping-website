package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "items", "items.product"})
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}
