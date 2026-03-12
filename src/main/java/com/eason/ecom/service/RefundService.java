package com.eason.ecom.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.RefundRequestCreateRequest;
import com.eason.ecom.dto.RefundRequestResponse;
import com.eason.ecom.dto.RefundSummaryResponse;
import com.eason.ecom.dto.RefundReviewRequest;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.RefundRequest;
import com.eason.ecom.entity.RefundStatus;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.messaging.OrderEventType;
import com.eason.ecom.messaging.OrderLifecycleEventFactory;
import com.eason.ecom.messaging.OrderLifecycleEventPublisher;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.RefundRequestRepository;

@Service
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final CommerceMetricsService commerceMetricsService;
    private final OrderLifecycleEventFactory orderLifecycleEventFactory;
    private final OrderLifecycleEventPublisher orderLifecycleEventPublisher;

    public RefundService(
            RefundRequestRepository refundRequestRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderService orderService,
            AuditLogService auditLogService,
            CommerceMetricsService commerceMetricsService,
            OrderLifecycleEventFactory orderLifecycleEventFactory,
            OrderLifecycleEventPublisher orderLifecycleEventPublisher) {
        this.refundRequestRepository = refundRequestRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
        this.commerceMetricsService = commerceMetricsService;
        this.orderLifecycleEventFactory = orderLifecycleEventFactory;
        this.orderLifecycleEventPublisher = orderLifecycleEventPublisher;
    }

    @Transactional
    public RefundRequestResponse createRefundRequest(
            Long userId,
            String username,
            Long orderId,
            RefundRequestCreateRequest request) {
        CustomerOrder customerOrder = customerOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateRefundEligibility(customerOrder);
        if (refundRequestRepository.existsByOrderIdAndRefundStatusIn(
                orderId,
                List.of(RefundStatus.REQUESTED, RefundStatus.APPROVED))) {
            throw new BadRequestException("An active refund request already exists for this order");
        }

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setOrder(customerOrder);
        refundRequest.setRequestedByUserId(userId);
        refundRequest.setRequestedByUsername(username);
        refundRequest.setRefundStatus(RefundStatus.REQUESTED);
        refundRequest.setReason(request.reason().trim());
        RefundRequest savedRequest = refundRequestRepository.save(refundRequest);

        auditLogService.recordUserAction(
                userId,
                username,
                "REFUND_REQUEST_CREATED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Refund requested for order " + customerOrder.getOrderNo(),
                Map.of(
                        "refundRequestId", savedRequest.getId(),
                        "reasonPreview", truncate(savedRequest.getReason())));
        commerceMetricsService.incrementRefundRequested();
        orderLifecycleEventPublisher.publish(orderLifecycleEventFactory.create(
                OrderEventType.REFUND_REQUEST_CREATED,
                customerOrder,
                "customer-refund-request",
                username,
                Map.of(
                        "refundRequestId", savedRequest.getId(),
                        "refundStatus", savedRequest.getRefundStatus().name(),
                        "reasonPreview", truncate(savedRequest.getReason()))));

        return toResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<RefundRequestResponse> getRefundRequestsForUser(Long userId, Long orderId) {
        CustomerOrder customerOrder = customerOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return refundRequestRepository.findByOrderIdOrderByRequestedAtDesc(customerOrder.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<RefundRequestResponse> getRefundRequestsForAdmin(String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<RefundRequest> refundPage;
        if (StringUtils.hasText(status)) {
            RefundStatus refundStatus = resolveRefundStatus(status);
            refundPage = refundRequestRepository.findByRefundStatusOrderByRequestedAtDesc(refundStatus, pageRequest);
        } else {
            refundPage = refundRequestRepository.findAllByOrderByRequestedAtDesc(pageRequest);
        }
        return new PagedResponse<>(
                refundPage.getContent().stream().map(this::toResponse).toList(),
                refundPage.getTotalElements(),
                refundPage.getTotalPages(),
                refundPage.getNumber(),
                refundPage.getSize());
    }

    @Transactional(readOnly = true)
    public RefundSummaryResponse getRefundSummary(LocalDate dateFrom, LocalDate dateTo) {
        LocalDateTime dateFromDateTime = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime dateToExclusive = dateTo == null ? null : dateTo.plusDays(1).atStartOfDay();

        long totalRequests = 0L;
        long requestedCount = 0L;
        long approvedCount = 0L;
        long rejectedCount = 0L;
        long settledCount = 0L;
        BigDecimal requestedAmount = BigDecimal.ZERO;
        BigDecimal approvedAmount = BigDecimal.ZERO;
        BigDecimal settledAmount = BigDecimal.ZERO;

        for (Object[] row : refundRequestRepository.summarizeByStatus(dateFromDateTime, dateToExclusive)) {
            RefundStatus refundStatus = (RefundStatus) row[0];
            long count = ((Number) row[1]).longValue();
            BigDecimal amount = row[2] instanceof BigDecimal bigDecimal
                    ? bigDecimal
                    : BigDecimal.valueOf(((Number) row[2]).doubleValue());

            totalRequests += count;
            switch (refundStatus) {
                case REQUESTED -> {
                    requestedCount = count;
                    requestedAmount = amount;
                }
                case APPROVED -> {
                    approvedCount = count;
                    approvedAmount = amount;
                }
                case REJECTED -> rejectedCount = count;
                case SETTLED -> {
                    settledCount = count;
                    settledAmount = amount;
                }
            }
        }

        return new RefundSummaryResponse(
                totalRequests,
                requestedCount,
                approvedCount,
                rejectedCount,
                settledCount,
                requestedAmount,
                approvedAmount,
                settledAmount);
    }

    @Transactional
    public RefundRequestResponse reviewRefundRequest(
            Long refundRequestId,
            RefundReviewRequest request,
            Long actorUserId,
            String actorUsername) {
        RefundRequest refundRequest = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
        if (refundRequest.getRefundStatus() != RefundStatus.REQUESTED) {
            throw new BadRequestException("Refund request has already been reviewed");
        }

        String decision = request.decision().trim().toUpperCase();
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new BadRequestException("Unsupported refund decision: " + request.decision());
        }

        refundRequest.setReviewedByUserId(actorUserId);
        refundRequest.setReviewedByUsername(actorUsername);
        refundRequest.setReviewedAt(LocalDateTime.now());
        refundRequest.setReviewNote(normalizeOptional(request.reviewNote()));
        if ("APPROVED".equals(decision)) {
            refundRequest.setRefundStatus(RefundStatus.APPROVED);
            CustomerOrder customerOrder = refundRequest.getOrder();
            if (customerOrder.getStatus() == OrderStatus.SHIPPED || customerOrder.getStatus() == OrderStatus.COMPLETED) {
                orderService.updateOrderStatusForAdmin(
                        customerOrder.getId(),
                        OrderStatus.REFUND_PENDING.name(),
                        "Refund approved for order " + customerOrder.getOrderNo(),
                        actorUserId,
                        actorUsername);
            }
        } else {
            refundRequest.setRefundStatus(RefundStatus.REJECTED);
        }

        RefundRequest savedRequest = refundRequestRepository.save(refundRequest);
        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "REFUND_REQUEST_REVIEWED",
                "ORDER",
                refundRequest.getOrder().getOrderNo(),
                "Refund request reviewed for order " + refundRequest.getOrder().getOrderNo(),
                Map.of(
                        "refundRequestId", savedRequest.getId(),
                        "decision", savedRequest.getRefundStatus().name(),
                        "reviewNote", savedRequest.getReviewNote() == null ? "" : savedRequest.getReviewNote()));
        commerceMetricsService.incrementRefundReviewed(savedRequest.getRefundStatus().name());
        orderLifecycleEventPublisher.publish(orderLifecycleEventFactory.create(
                OrderEventType.REFUND_REQUEST_REVIEWED,
                refundRequest.getOrder(),
                "admin-refund-review",
                actorUsername,
                Map.of(
                        "refundRequestId", savedRequest.getId(),
                        "decision", savedRequest.getRefundStatus().name(),
                        "reviewNote", savedRequest.getReviewNote() == null ? "" : savedRequest.getReviewNote())));

        return toResponse(savedRequest);
    }

    @Transactional
    public void markSettledForOrder(CustomerOrder customerOrder) {
        refundRequestRepository.findByOrderIdOrderByRequestedAtDesc(customerOrder.getId()).stream()
                .filter(refundRequest -> refundRequest.getRefundStatus() == RefundStatus.APPROVED)
                .findFirst()
                .ifPresent(refundRequest -> {
                    refundRequest.setRefundStatus(RefundStatus.SETTLED);
                    refundRequest.setReviewedAt(LocalDateTime.now());
                    refundRequestRepository.save(refundRequest);
                    auditLogService.recordUserAction(
                            null,
                            "payment-callback",
                            "REFUND_SETTLED",
                            "ORDER",
                            customerOrder.getOrderNo(),
                            "Refund settled for order " + customerOrder.getOrderNo(),
                            Map.of("refundRequestId", refundRequest.getId()));
                    commerceMetricsService.incrementRefundSettled();
                    orderLifecycleEventPublisher.publish(orderLifecycleEventFactory.create(
                            OrderEventType.REFUND_SETTLED,
                            customerOrder,
                            "payment-callback",
                            "payment-callback",
                            Map.of(
                                    "refundRequestId", refundRequest.getId(),
                                    "refundStatus", refundRequest.getRefundStatus().name())));
                });
    }

    private void validateRefundEligibility(CustomerOrder customerOrder) {
        if (customerOrder.getStatus() != OrderStatus.SHIPPED && customerOrder.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Refund can only be requested for shipped or completed orders");
        }
    }

    private RefundStatus resolveRefundStatus(String status) {
        try {
            return RefundStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Unsupported refund status: " + status);
        }
    }

    private RefundRequestResponse toResponse(RefundRequest refundRequest) {
        return new RefundRequestResponse(
                refundRequest.getId(),
                refundRequest.getOrder().getId(),
                refundRequest.getOrder().getOrderNo(),
                refundRequest.getRefundStatus().name(),
                refundRequest.getReason(),
                refundRequest.getReviewNote(),
                refundRequest.getRequestedByUserId(),
                refundRequest.getRequestedByUsername(),
                refundRequest.getReviewedByUserId(),
                refundRequest.getReviewedByUsername(),
                refundRequest.getRequestedAt(),
                refundRequest.getReviewedAt());
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String text) {
        return text.length() <= 120 ? text : text.substring(0, 120);
    }
}
