package com.eason.ecom.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eason.ecom.entity.SupportTicket;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    List<SupportTicket> findByOrderIdAndRequestedByUserIdOrderByCreatedAtDesc(Long orderId, Long requestedByUserId);

    @EntityGraph(attributePaths = {"order", "order.user"})
    @Query("select supportTicket from SupportTicket supportTicket where supportTicket.id = :ticketId")
    Optional<SupportTicket> findWithDetailsById(@Param("ticketId") Long ticketId);

    @Query("""
            select supportTicket.ticketStatus, count(supportTicket)
            from SupportTicket supportTicket
            group by supportTicket.ticketStatus
            """)
    List<Object[]> summarizeByStatus();

    @Query("""
            select count(supportTicket)
            from SupportTicket supportTicket
            where supportTicket.ticketStatus in :statuses
            """)
    long countByTicketStatusIn(@Param("statuses") Collection<com.eason.ecom.entity.SupportTicketStatus> statuses);

    @Query("""
            select count(supportTicket)
            from SupportTicket supportTicket
            where supportTicket.priority = :priority
              and supportTicket.ticketStatus in :statuses
            """)
    long countByPriorityAndTicketStatusIn(
            @Param("priority") com.eason.ecom.entity.SupportTicketPriority priority,
            @Param("statuses") Collection<com.eason.ecom.entity.SupportTicketStatus> statuses);

    @Override
    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<SupportTicket> findAll(org.springframework.data.jpa.domain.Specification<SupportTicket> spec, Pageable pageable);
}
