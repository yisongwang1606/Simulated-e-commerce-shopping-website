package com.eason.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.InventoryAdjustment;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    @EntityGraph(attributePaths = "product")
    List<InventoryAdjustment> findTop100ByProductIdOrderByCreatedAtDesc(Long productId);
}
