package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "SupportTicketUpdateRequest", description = "Admin support ticket update payload")
public record SupportTicketUpdateRequest(
        @Schema(description = "Target ticket status", example = "IN_PROGRESS")
        String status,

        @Schema(description = "Assigned team", example = "Customer Support")
        @Size(max = 60, message = "assignedTeam must be at most 60 characters")
        String assignedTeam,

        @Schema(description = "Assigned operator username", example = "support.alex")
        @Size(max = 50, message = "assignedToUsername must be at most 50 characters")
        String assignedToUsername,

        @Schema(description = "Latest operational note", example = "Customer asked for a doorstep delivery investigation with carrier.")
        @Size(max = 500, message = "latestNote must be at most 500 characters")
        String latestNote,

        @Schema(description = "Resolution note for resolved or closed tickets", example = "Carrier confirmed mis-scan and redelivered the next day.")
        @Size(max = 500, message = "resolutionNote must be at most 500 characters")
        String resolutionNote) {
}
