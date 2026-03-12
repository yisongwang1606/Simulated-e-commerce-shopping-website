package com.eason.ecom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.UserProfileResponse;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.service.AuthService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Authenticated user profile endpoints")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Get the current user profile",
            description = "Returns the authenticated user's profile details resolved from the JWT token.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponseFactory.ok(authService.getUserProfile(authenticatedUser.getId()));
    }
}
