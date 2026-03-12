package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RefundRequestCreateRequest", description = "Customer refund request payload")
public record RefundRequestCreateRequest(
        @Schema(description = "Customer reason for the refund", example = "The parcel arrived damaged and the item stopped working after first use.")
        @NotBlank(message = "reason must not be blank")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason) {
}
