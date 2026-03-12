package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CustomerAddressRequest", description = "Customer shipping address payload")
public record CustomerAddressRequest(
        @Schema(description = "Short label for the address", example = "Home")
        @NotBlank(message = "addressLabel must not be blank")
        @Size(max = 40, message = "addressLabel must be at most 40 characters")
        String addressLabel,

        @Schema(description = "Receiver full name", example = "Alex Morgan")
        @NotBlank(message = "receiverName must not be blank")
        @Size(max = 80, message = "receiverName must be at most 80 characters")
        String receiverName,

        @Schema(description = "Phone number", example = "+1-403-555-0199")
        @NotBlank(message = "phone must not be blank")
        @Size(max = 25, message = "phone must be at most 25 characters")
        String phone,

        @Schema(description = "Primary street line", example = "1200 4 Ave SW")
        @NotBlank(message = "line1 must not be blank")
        @Size(max = 120, message = "line1 must be at most 120 characters")
        String line1,

        @Schema(description = "Secondary street line", example = "Unit 1806")
        @Size(max = 120, message = "line2 must be at most 120 characters")
        String line2,

        @Schema(description = "City", example = "Calgary")
        @NotBlank(message = "city must not be blank")
        @Size(max = 80, message = "city must be at most 80 characters")
        String city,

        @Schema(description = "Province or territory", example = "Alberta")
        @NotBlank(message = "province must not be blank")
        @Size(max = 80, message = "province must be at most 80 characters")
        String province,

        @Schema(description = "Postal code", example = "T2P 2S6")
        @NotBlank(message = "postalCode must not be blank")
        @Size(max = 20, message = "postalCode must be at most 20 characters")
        String postalCode,

        @Schema(description = "Whether to make this the default shipping address", example = "true")
        Boolean isDefault) {
}
