package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eason.ecom.entity.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Shipment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<Shipment> findByOrderIdAndOrderUserIdOrderByCreatedAtDesc(Long orderId, Long userId);

    @EntityGraph(attributePaths = {"order", "order.user"})
    @Query("select shipment from Shipment shipment where shipment.id = :id")
    Optional<Shipment> findWithDetailsById(@Param("id") Long id);
}
