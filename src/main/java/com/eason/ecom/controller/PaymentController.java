package com.eason.ecom.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.PaymentCallbackRequest;
import com.eason.ecom.dto.PaymentTransactionResponse;
import com.eason.ecom.service.PaymentService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment integration placeholder endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final AppProperties appProperties;

    public PaymentController(PaymentService paymentService, AppProperties appProperties) {
        this.paymentService = paymentService;
        this.appProperties = appProperties;
    }

    @Operation(
            summary = "Process a simulated payment gateway callback",
            description = "Accepts a mock gateway callback and updates the related payment transaction and order status.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Callback processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Callback token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment transaction not found")
    })
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> processCallback(
        @RequestHeader(name = "X-Callback-Token", required = false) String callbackToken,
        @RequestBody @Validated PaymentCallbackRequest request) {
        if (!appProperties.getIntegrations().getPaymentCallbackToken().equals(callbackToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid callback token", null));
        }
        return ApiResponseFactory.ok(
                "Payment callback processed successfully",
                paymentService.handleCallback(request));
    }
}
