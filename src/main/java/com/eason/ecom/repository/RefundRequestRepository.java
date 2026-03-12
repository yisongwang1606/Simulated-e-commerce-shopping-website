package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select refundRequest.refundStatus,
                   count(refundRequest),
                   coalesce(sum(refundRequest.order.totalAmount), 0)
            from RefundRequest refundRequest
            where (:dateFrom is null or refundRequest.requestedAt >= :dateFrom)
              and (:dateToExclusive is null or refundRequest.requestedAt < :dateToExclusive)
            group by refundRequest.refundStatus
            """)
    List<Object[]> summarizeByStatus(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateToExclusive") LocalDateTime dateToExclusive);
}
