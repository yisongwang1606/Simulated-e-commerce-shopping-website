package com.eason.ecom.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "JWT authentication response")
public record AuthResponse(
        String token,
        Instant expiresAt,
        UserProfileResponse user) {
}
