package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ShipmentCreateRequest", description = "Shipment creation request for a paid order")
public record ShipmentCreateRequest(
        @Schema(description = "Carrier code", example = "CANADA_POST")
        @NotBlank(message = "carrierCode must not be blank")
        String carrierCode,

        @Schema(description = "Tracking number", example = "CP123456789CA")
        @NotBlank(message = "trackingNo must not be blank")
        @Size(max = 64, message = "trackingNo must be at most 64 characters")
        String trackingNo,

        @Schema(description = "Shipment note", example = "Packed in warehouse zone A and handed to carrier pickup")
        @Size(max = 255, message = "note must be at most 255 characters")
        String note) {
}
