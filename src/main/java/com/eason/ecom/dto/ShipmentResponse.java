package com.eason.ecom.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ShipmentResponse", description = "Shipment placeholder record")
public record ShipmentResponse(
        Long id,
        Long orderId,
        String orderNo,
        String shipmentNo,
        String carrierCode,
        String trackingNo,
        String shipmentStatus,
        String statusNote,
        LocalDateTime createdAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime updatedAt) {
}
