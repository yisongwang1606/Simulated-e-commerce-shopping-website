package com.eason.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.OrderInternalNote;

public interface OrderInternalNoteRepository extends JpaRepository<OrderInternalNote, Long> {

    @EntityGraph(attributePaths = "order")
    List<OrderInternalNote> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
