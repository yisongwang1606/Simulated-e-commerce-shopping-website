package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "OrderInternalNoteRequest", description = "Admin-only internal note for operational follow-up")
public record OrderInternalNoteRequest(
        @Schema(description = "Internal note text", example = "Customer requested weekday delivery only; support to follow up before dispatch.")
        @NotBlank(message = "noteText must not be blank")
        @Size(max = 500, message = "noteText must be at most 500 characters")
        String noteText) {
}
