package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eason.ecom.entity.RefundRequest;
import com.eason.ecom.entity.RefundStatus;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<RefundRequest> findByOrderIdOrderByRequestedAtDesc(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<RefundRequest> findByRefundStatusOrderByRequestedAtDesc(RefundStatus refundStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<RefundRequest> findAllByOrderByRequestedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<RefundRequest> findById(Long id);

    boolean existsByOrderIdAndRefundStatusIn(Long orderId, List<RefundStatus> statuses);
}
