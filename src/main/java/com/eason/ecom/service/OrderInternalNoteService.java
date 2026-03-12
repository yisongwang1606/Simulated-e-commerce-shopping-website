package com.eason.ecom.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.dto.OrderInternalNoteRequest;
import com.eason.ecom.dto.OrderInternalNoteResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderInternalNote;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.OrderInternalNoteRepository;

@Service
public class OrderInternalNoteService {

    private final OrderInternalNoteRepository orderInternalNoteRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final AuditLogService auditLogService;

    public OrderInternalNoteService(
            OrderInternalNoteRepository orderInternalNoteRepository,
            CustomerOrderRepository customerOrderRepository,
            AuditLogService auditLogService) {
        this.orderInternalNoteRepository = orderInternalNoteRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OrderInternalNoteResponse addNote(
            Long orderId,
            OrderInternalNoteRequest request,
            Long actorUserId,
            String actorUsername) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderInternalNote note = new OrderInternalNote();
        note.setOrder(customerOrder);
        note.setNoteText(request.noteText().trim());
        note.setCreatedByUserId(actorUserId);
        note.setCreatedByUsername(actorUsername);
        OrderInternalNote savedNote = orderInternalNoteRepository.save(note);

        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "ORDER_INTERNAL_NOTE_ADDED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Internal note added to order " + customerOrder.getOrderNo(),
                Map.of(
                        "noteId", savedNote.getId(),
                        "notePreview", truncate(savedNote.getNoteText())));

        return toResponse(savedNote);
    }

    @Transactional(readOnly = true)
    public List<OrderInternalNoteResponse> getNotes(Long orderId) {
        return orderInternalNoteRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderInternalNoteResponse toResponse(OrderInternalNote note) {
        return new OrderInternalNoteResponse(
                note.getId(),
                note.getOrder().getId(),
                note.getOrder().getOrderNo(),
                note.getNoteText(),
                note.getCreatedByUserId(),
                note.getCreatedByUsername(),
                note.getCreatedAt());
    }

    private String truncate(String text) {
        return text.length() <= 80 ? text : text.substring(0, 80);
    }
}
