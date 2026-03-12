package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserProfileResponse", description = "Public profile information for the authenticated user")
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt) {
}
