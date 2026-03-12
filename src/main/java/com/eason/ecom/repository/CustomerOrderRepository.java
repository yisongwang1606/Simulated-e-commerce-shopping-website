package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eason.ecom.entity.CustomerOrder;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    @EntityGraph(attributePaths = {"user", "items", "items.product", "tagAssignments", "tagAssignments.orderTag"})
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "items", "items.product", "tagAssignments", "tagAssignments.orderTag"})
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"user", "items", "items.product", "tagAssignments", "tagAssignments.orderTag"})
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "items", "items.product", "tagAssignments", "tagAssignments.orderTag"})
    @Query("select customerOrder from CustomerOrder customerOrder where customerOrder.id = :id")
    Optional<CustomerOrder> findWithDetailsById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "items", "items.product", "tagAssignments", "tagAssignments.orderTag"})
    Page<CustomerOrder> findAll(org.springframework.data.jpa.domain.Specification<CustomerOrder> spec, Pageable pageable);
}
