package com.eason.ecom.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.SupportTicketCreateRequest;
import com.eason.ecom.dto.SupportTicketResponse;
import com.eason.ecom.dto.SupportTicketUpdateRequest;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.SupportTicket;
import com.eason.ecom.entity.SupportTicketPriority;
import com.eason.ecom.entity.SupportTicketStatus;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.SupportTicketRepository;
import com.eason.ecom.support.SupportTicketNumberGenerator;

import jakarta.persistence.criteria.Predicate;

@Service
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final SupportTicketNumberGenerator supportTicketNumberGenerator;
    private final AuditLogService auditLogService;

    public SupportTicketService(
            SupportTicketRepository supportTicketRepository,
            CustomerOrderRepository customerOrderRepository,
            SupportTicketNumberGenerator supportTicketNumberGenerator,
            AuditLogService auditLogService) {
        this.supportTicketRepository = supportTicketRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.supportTicketNumberGenerator = supportTicketNumberGenerator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SupportTicketResponse createTicket(
            Long userId,
            String username,
            Long orderId,
            SupportTicketCreateRequest request) {
        CustomerOrder customerOrder = customerOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setOrder(customerOrder);
        supportTicket.setTicketNo(supportTicketNumberGenerator.next());
        supportTicket.setRequestedByUserId(userId);
        supportTicket.setRequestedByUsername(username);
        supportTicket.setTicketStatus(SupportTicketStatus.OPEN);
        supportTicket.setPriority(resolvePriority(request.priority()));
        supportTicket.setCategory(normalizeRequired(request.category()));
        supportTicket.setSubject(normalizeRequired(request.subject()));
        supportTicket.setCustomerMessage(normalizeRequired(request.customerMessage()));

        SupportTicket savedTicket = supportTicketRepository.save(supportTicket);
        auditLogService.recordUserAction(
                userId,
                username,
                "SUPPORT_TICKET_CREATED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Support ticket " + savedTicket.getTicketNo() + " created for " + customerOrder.getOrderNo(),
                Map.of(
                        "ticketId", savedTicket.getId(),
                        "ticketNo", savedTicket.getTicketNo(),
                        "priority", savedTicket.getPriority().name(),
                        "category", savedTicket.getCategory()));

        return toResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getTicketsForUser(Long userId, Long orderId) {
        return supportTicketRepository.findByOrderIdAndRequestedByUserIdOrderByCreatedAtDesc(orderId, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<SupportTicketResponse> getTicketsForAdmin(
            String status,
            String priority,
            String assignedTeam,
            int page,
            int size) {
        Page<SupportTicket> ticketPage = supportTicketRepository.findAll(
                buildSpecification(status, priority, assignedTeam),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new PagedResponse<>(
                ticketPage.getContent().stream().map(this::toResponse).toList(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.getNumber(),
                ticketPage.getSize());
    }

    @Transactional
    public SupportTicketResponse updateTicket(
            Long ticketId,
            SupportTicketUpdateRequest request,
            Long actorUserId,
            String actorUsername) {
        SupportTicket supportTicket = supportTicketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));

        if (StringUtils.hasText(request.status())) {
            SupportTicketStatus nextStatus = resolveStatus(request.status());
            supportTicket.setTicketStatus(nextStatus);
            if (nextStatus == SupportTicketStatus.RESOLVED || nextStatus == SupportTicketStatus.CLOSED) {
                supportTicket.setResolvedAt(LocalDateTime.now());
            } else {
                supportTicket.setResolvedAt(null);
            }
        }

        supportTicket.setAssignedTeam(normalizeOptional(request.assignedTeam()));
        supportTicket.setAssignedToUsername(normalizeOptional(request.assignedToUsername()));
        supportTicket.setLatestNote(normalizeOptional(request.latestNote()));
        supportTicket.setResolutionNote(normalizeOptional(request.resolutionNote()));

        SupportTicket savedTicket = supportTicketRepository.save(supportTicket);
        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "SUPPORT_TICKET_UPDATED",
                "ORDER",
                savedTicket.getOrder().getOrderNo(),
                "Support ticket " + savedTicket.getTicketNo() + " updated",
                Map.of(
                        "ticketId", savedTicket.getId(),
                        "ticketNo", savedTicket.getTicketNo(),
                        "status", savedTicket.getTicketStatus().name(),
                        "assignedTeam", savedTicket.getAssignedTeam() == null ? "" : savedTicket.getAssignedTeam()));

        return toResponse(savedTicket);
    }

    private Specification<SupportTicket> buildSpecification(
            String status,
            String priority,
            String assignedTeam) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (StringUtils.hasText(status)) {
                predicates.add(criteriaBuilder.equal(root.get("ticketStatus"), resolveStatus(status)));
            }
            if (StringUtils.hasText(priority)) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), resolvePriority(priority)));
            }
            if (StringUtils.hasText(assignedTeam)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("assignedTeam")),
                        "%" + assignedTeam.trim().toLowerCase() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private SupportTicketResponse toResponse(SupportTicket supportTicket) {
        return new SupportTicketResponse(
                supportTicket.getId(),
                supportTicket.getOrder().getId(),
                supportTicket.getOrder().getOrderNo(),
                supportTicket.getTicketNo(),
                supportTicket.getTicketStatus().name(),
                supportTicket.getPriority().name(),
                supportTicket.getCategory(),
                supportTicket.getSubject(),
                supportTicket.getCustomerMessage(),
                supportTicket.getLatestNote(),
                supportTicket.getAssignedTeam(),
                supportTicket.getAssignedToUsername(),
                supportTicket.getResolutionNote(),
                supportTicket.getRequestedByUserId(),
                supportTicket.getRequestedByUsername(),
                supportTicket.getCreatedAt(),
                supportTicket.getUpdatedAt(),
                supportTicket.getResolvedAt());
    }

    private SupportTicketStatus resolveStatus(String status) {
        try {
            return SupportTicketStatus.valueOf(normalizeRequired(status).toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Unsupported support ticket status: " + status);
        }
    }

    private SupportTicketPriority resolvePriority(String priority) {
        try {
            return SupportTicketPriority.valueOf(normalizeRequired(priority).toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Unsupported support ticket priority: " + priority);
        }
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
