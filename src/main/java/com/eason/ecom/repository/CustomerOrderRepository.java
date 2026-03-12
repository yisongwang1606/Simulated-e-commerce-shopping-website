package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.time.LocalDateTime;

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

    @Query("""
            select customerOrder.status, count(customerOrder)
            from CustomerOrder customerOrder
            group by customerOrder.status
            """)
    List<Object[]> summarizeByStatus();

    @Query("""
            select count(customerOrder)
            from CustomerOrder customerOrder
            where customerOrder.createdAt >= :createdAt
            """)
    long countCreatedSince(@Param("createdAt") LocalDateTime createdAt);

    @Query("""
            select count(customerOrder)
            from CustomerOrder customerOrder
            where customerOrder.status in :statuses
            """)
    long countByStatusIn(@Param("statuses") Collection<com.eason.ecom.entity.OrderStatus> statuses);

    @Query("""
            select coalesce(sum(customerOrder.totalAmount), 0), count(customerOrder)
            from CustomerOrder customerOrder
            where customerOrder.createdAt >= :createdAt
              and customerOrder.status in :statuses
            """)
    List<Object[]> summarizeCommercialWindow(
            @Param("createdAt") LocalDateTime createdAt,
            @Param("statuses") Collection<com.eason.ecom.entity.OrderStatus> statuses);

    @Override
    @EntityGraph(attributePaths = {"user", "items", "items.product", "tagAssignments", "tagAssignments.orderTag"})
    Page<CustomerOrder> findAll(org.springframework.data.jpa.domain.Specification<CustomerOrder> spec, Pageable pageable);
}
