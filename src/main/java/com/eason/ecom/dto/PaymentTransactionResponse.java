package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaymentTransactionResponse", description = "Payment transaction placeholder record")
public record PaymentTransactionResponse(
        Long id,
        Long orderId,
        String orderNo,
        String paymentMethod,
        String paymentStatus,
        String transactionRef,
        String providerCode,
        String providerEventId,
        String providerReference,
        BigDecimal amount,
        String clientSecret,
        String note,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime updatedAt) {
}
