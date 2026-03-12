package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.CustomerAddress;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    @EntityGraph(attributePaths = "user")
    List<CustomerAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<CustomerAddress> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<CustomerAddress> findByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);
}
