package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CustomerAddressResponse", description = "Customer shipping address")
public record CustomerAddressResponse(
        Long id,
        String addressLabel,
        String receiverName,
        String phone,
        String line1,
        String line2,
        String city,
        String province,
        String postalCode,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
