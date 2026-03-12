package com.eason.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Payload for logging in with username or email")
public record LoginRequest(
        @Schema(description = "Username or email", example = "Jack@example.com")
        @NotBlank String username,
        @Schema(description = "Account password", example = "123456")
        @NotBlank String password) {
}
