package com.eason.ecom.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        Instant expiresAt,
        UserProfileResponse user) {
}
