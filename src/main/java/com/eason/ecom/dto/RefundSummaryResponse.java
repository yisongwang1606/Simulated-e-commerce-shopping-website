package com.eason.ecom.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefundSummaryResponse", description = "Refund operations summary for dashboard use")
public record RefundSummaryResponse(
        long totalRequests,
        long requestedCount,
        long approvedCount,
        long rejectedCount,
        long settledCount,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal settledAmount) {
}
