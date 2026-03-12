package com.eason.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.CreateOrderRequest;
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.dto.RefundRequestCreateRequest;
import com.eason.ecom.dto.RefundRequestResponse;
import com.eason.ecom.dto.ShipmentResponse;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.service.OrderService;
import com.eason.ecom.service.RefundService;
import com.eason.ecom.service.ShipmentService;
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
    private final ShipmentService shipmentService;
    private final RefundService refundService;

    public OrderController(OrderService orderService, ShipmentService shipmentService, RefundService refundService) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
        this.refundService = refundService;
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody(required = false) @Validated CreateOrderRequest request) {
        return ApiResponseFactory.created(
                "Order created successfully",
                orderService.createOrder(
                        authenticatedUser.getId(),
                        request == null ? null : request.addressId()));
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

    @Operation(
            summary = "Get shipment records for one order",
            description = "Returns shipment tracking placeholders for an order owned by the authenticated user.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipments loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/{orderId}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getShipments(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(shipmentService.getShipmentsForCustomer(authenticatedUser.getId(), orderId));
    }

    @Operation(
            summary = "Create a refund request for one order",
            description = "Allows the authenticated customer to request a refund for a shipped or completed order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Refund request created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Refund request is not allowed for the current order state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/{orderId}/refund-requests")
    public ResponseEntity<ApiResponse<RefundRequestResponse>> createRefundRequest(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @RequestBody @Validated RefundRequestCreateRequest request) {
        return ApiResponseFactory.created(
                "Refund request created successfully",
                refundService.createRefundRequest(
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername(),
                        orderId,
                        request));
    }

    @Operation(
            summary = "List refund requests for one order",
            description = "Returns refund requests submitted by the authenticated user for a specific order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund requests loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}/refund-requests")
    public ResponseEntity<ApiResponse<List<RefundRequestResponse>>> getRefundRequests(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(refundService.getRefundRequestsForUser(authenticatedUser.getId(), orderId));
    }
}
