package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eason.ecom.entity.OrderTagAssignment;

public interface OrderTagAssignmentRepository extends JpaRepository<OrderTagAssignment, Long> {

    @Query("""
            select assignment
            from OrderTagAssignment assignment
            join fetch assignment.orderTag
            where assignment.order.id = :orderId
            order by assignment.createdAt asc
            """)
    List<OrderTagAssignment> findByOrderIdWithTag(@Param("orderId") Long orderId);

    Optional<OrderTagAssignment> findByOrderIdAndOrderTagId(Long orderId, Long orderTagId);
}
