package com.eason.ecom.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.dto.OrderTagResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderTag;
import com.eason.ecom.entity.OrderTagAssignment;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.OrderTagAssignmentRepository;
import com.eason.ecom.repository.OrderTagRepository;

@Service
public class OrderTagService {

    private final OrderTagRepository orderTagRepository;
    private final OrderTagAssignmentRepository orderTagAssignmentRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final AuditLogService auditLogService;

    public OrderTagService(
            OrderTagRepository orderTagRepository,
            OrderTagAssignmentRepository orderTagAssignmentRepository,
            CustomerOrderRepository customerOrderRepository,
            AuditLogService auditLogService) {
        this.orderTagRepository = orderTagRepository;
        this.orderTagAssignmentRepository = orderTagAssignmentRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<OrderTagResponse> getOrderTagCatalog() {
        return orderTagRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderTagResponse> getOrderTags(Long orderId) {
        ensureOrderExists(orderId);
        return orderTagAssignmentRepository.findByOrderIdWithTag(orderId).stream()
                .map(assignment -> toResponse(assignment.getOrderTag()))
                .toList();
    }

    @Transactional
    public List<OrderTagResponse> assignTag(Long orderId, Long orderTagId, Long actorUserId, String actorUsername) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderTag orderTag = orderTagRepository.findById(orderTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Order tag not found"));

        orderTagAssignmentRepository.findByOrderIdAndOrderTagId(orderId, orderTagId)
                .orElseGet(() -> {
                    OrderTagAssignment assignment = new OrderTagAssignment();
                    assignment.setOrder(customerOrder);
                    assignment.setOrderTag(orderTag);
                    assignment.setAssignedByUserId(actorUserId);
                    assignment.setAssignedByUsername(actorUsername);
                    auditLogService.recordUserAction(
                            actorUserId,
                            actorUsername,
                            "ORDER_TAG_ASSIGNED",
                            "ORDER",
                            customerOrder.getOrderNo(),
                            "Assigned tag " + orderTag.getTagCode() + " to " + customerOrder.getOrderNo(),
                            Map.of(
                                    "orderTagId", orderTag.getId(),
                                    "tagCode", orderTag.getTagCode()));
                    return orderTagAssignmentRepository.save(assignment);
                });

        return orderTagAssignmentRepository.findByOrderIdWithTag(orderId).stream()
                .map(assignment -> toResponse(assignment.getOrderTag()))
                .toList();
    }

    @Transactional
    public List<OrderTagResponse> removeTag(Long orderId, Long orderTagId, Long actorUserId, String actorUsername) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderTagAssignment assignment = orderTagAssignmentRepository.findByOrderIdAndOrderTagId(orderId, orderTagId)
                .orElseThrow(() -> new ResourceNotFoundException("Order tag assignment not found"));

        orderTagAssignmentRepository.delete(assignment);
        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "ORDER_TAG_REMOVED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Removed tag " + assignment.getOrderTag().getTagCode() + " from " + customerOrder.getOrderNo(),
                Map.of(
                        "orderTagId", assignment.getOrderTag().getId(),
                        "tagCode", assignment.getOrderTag().getTagCode()));

        return orderTagAssignmentRepository.findByOrderIdWithTag(orderId).stream()
                .map(existingAssignment -> toResponse(existingAssignment.getOrderTag()))
                .toList();
    }

    private void ensureOrderExists(Long orderId) {
        if (!customerOrderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found");
        }
    }

    private OrderTagResponse toResponse(OrderTag orderTag) {
        return new OrderTagResponse(
                orderTag.getId(),
                orderTag.getTagCode(),
                orderTag.getDisplayName(),
                orderTag.getTagGroup(),
                orderTag.getTone());
    }
}
