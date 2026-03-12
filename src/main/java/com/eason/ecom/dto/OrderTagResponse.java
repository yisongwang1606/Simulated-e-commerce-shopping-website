package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderTagResponse", description = "Operational tag assigned to an order")
public record OrderTagResponse(
        Long id,
        String tagCode,
        String displayName,
        String tagGroup,
        String tone) {
}
