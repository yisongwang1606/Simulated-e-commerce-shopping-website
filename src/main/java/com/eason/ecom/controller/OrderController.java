package com.eason.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.service.OrderService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Customer order creation and order history endpoints")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Create an order from the current cart",
            description = "Reads the authenticated user's Redis cart, validates stock, creates an order, and clears the cart.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cart is empty or stock validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponseFactory.created("Order created successfully", orderService.createOrder(authenticatedUser.getId()));
    }

    @Operation(
            summary = "List the current user's orders",
            description = "Returns the authenticated user's order history in reverse chronological order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponseFactory.ok(orderService.getOrdersForUser(authenticatedUser.getId()));
    }

    @Operation(
            summary = "Get a single order",
            description = "Returns the details of one order owned by the authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(orderService.getOrderForUser(authenticatedUser.getId(), orderId));
    }
}
