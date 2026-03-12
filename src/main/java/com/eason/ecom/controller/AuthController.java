package com.eason.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.AuthResponse;
import com.eason.ecom.dto.LoginRequest;
import com.eason.ecom.dto.RegisterRequest;
import com.eason.ecom.dto.UserProfileResponse;
import com.eason.ecom.service.AuthService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration, login, logout, and token bootstrap endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a customer account",
            description = "Creates a new customer account. This endpoint is public and does not require a JWT.",
            security = {})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or username/email already exists",
                    content = @Content(schema = @Schema(implementation = com.eason.ecom.dto.ApiResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(@RequestBody @Validated RegisterRequest request) {
        return ApiResponseFactory.created("User registered successfully", authService.register(request));
    }

    @Operation(
            summary = "Login with username or email",
            description = "Validates credentials and returns a JWT token used for protected API calls.",
            security = {})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials or malformed body")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody @Validated LoginRequest request) {
        return ApiResponseFactory.ok("Login successful", authService.login(request));
    }

    @Operation(
            summary = "Logout the current user",
            description = "Revokes the current JWT token from Redis so it can no longer be used.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or malformed Bearer token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ApiResponseFactory.okMessage("Logout successful");
    }
}
