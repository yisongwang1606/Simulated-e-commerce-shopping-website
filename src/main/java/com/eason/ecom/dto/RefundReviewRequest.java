package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RefundReviewRequest", description = "Admin refund review decision")
public record RefundReviewRequest(
        @Schema(description = "Review decision", example = "APPROVED")
        @NotBlank(message = "decision must not be blank")
        String decision,

        @Schema(description = "Operational review note", example = "Damage photos validated by support; refund approved.")
        @Size(max = 500, message = "reviewNote must be at most 500 characters")
        String reviewNote) {
}
