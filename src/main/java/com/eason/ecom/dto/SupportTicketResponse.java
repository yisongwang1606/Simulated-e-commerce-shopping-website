package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SupportTicketResponse", description = "Customer support ticket details")
public record SupportTicketResponse(
        Long id,
        Long orderId,
        String orderNo,
        String ticketNo,
        String ticketStatus,
        String priority,
        String category,
        String subject,
        String customerMessage,
        String latestNote,
        String assignedTeam,
        String assignedToUsername,
        String resolutionNote,
        Long requestedByUserId,
        String requestedByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt) {
}
