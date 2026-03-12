package com.eason.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.CustomerAddressRequest;
import com.eason.ecom.dto.CustomerAddressResponse;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.service.AddressService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Addresses", description = "Customer shipping address book endpoints")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(
            summary = "List the current user's addresses",
            description = "Returns the authenticated user's shipping addresses, with the default address first.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Addresses loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getAddresses(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponseFactory.ok(addressService.getAddressesForUser(authenticatedUser.getId()));
    }

    @Operation(
            summary = "Create a shipping address",
            description = "Creates a shipping address for the authenticated user and can mark it as default.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Address created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid address payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> createAddress(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Validated CustomerAddressRequest request) {
        return ApiResponseFactory.created(
                "Address created successfully",
                addressService.createAddress(authenticatedUser.getId(), request));
    }

    @Operation(
            summary = "Update a shipping address",
            description = "Updates a shipping address owned by the authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid address payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> updateAddress(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address identifier", example = "1")
            @PathVariable @Positive Long addressId,
            @RequestBody @Validated CustomerAddressRequest request) {
        return ApiResponseFactory.ok(
                "Address updated successfully",
                addressService.updateAddress(authenticatedUser.getId(), addressId, request));
    }

    @Operation(
            summary = "Set the default shipping address",
            description = "Marks one address as the default shipping address for the authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Default address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @PutMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address identifier", example = "1")
            @PathVariable @Positive Long addressId) {
        return ApiResponseFactory.ok(
                "Default address updated successfully",
                addressService.setDefaultAddress(authenticatedUser.getId(), addressId));
    }

    @Operation(
            summary = "Delete a shipping address",
            description = "Deletes an address owned by the authenticated user and promotes another address to default if needed.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Address deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Address not found")
    })
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Address identifier", example = "1")
            @PathVariable @Positive Long addressId) {
        addressService.deleteAddress(authenticatedUser.getId(), addressId);
        return ApiResponseFactory.okMessage("Address deleted successfully");
    }
}
