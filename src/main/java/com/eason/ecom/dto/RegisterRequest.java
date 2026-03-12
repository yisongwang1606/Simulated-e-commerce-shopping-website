package com.eason.ecom.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegisterRequest", description = "Payload for creating a new customer account")
public record RegisterRequest(
        @Schema(description = "Unique username used for login", example = "alice")
        @NotBlank @Size(min = 3, max = 50) String username,
        @Schema(description = "Unique email address", example = "alice@example.com")
        @NotBlank @Email @Size(max = 120) String email,
        @Schema(description = "Account password", example = "Alice123!")
        @NotBlank @Size(min = 6, max = 100) String password) {
}
