package com.eason.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.dto.ProductRequest;
import com.eason.ecom.dto.ProductResponse;
import com.eason.ecom.service.OrderService;
import com.eason.ecom.service.ProductService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrator-only catalog and order management endpoints")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;

    public AdminController(ProductService productService, OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
    }

    @Operation(
            summary = "Create a product",
            description = "Creates a new product in the catalog. Requires an ADMIN JWT.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestBody @Validated ProductRequest request) {
        return ApiResponseFactory.created("Product created successfully", productService.createProduct(request));
    }

    @Operation(
            summary = "Update a product",
            description = "Updates an existing product in the catalog. Requires an ADMIN JWT.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PutMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId,
            @RequestBody @Validated ProductRequest request) {
        return ApiResponseFactory.ok("Product updated successfully", productService.updateProduct(productId, request));
    }

    @Operation(
            summary = "Delete a product",
            description = "Deletes a product and removes its cache entries. Requires an ADMIN JWT.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId) {
        productService.deleteProduct(productId);
        return ApiResponseFactory.okMessage("Product deleted successfully");
    }

    @Operation(
            summary = "List all orders",
            description = "Returns all customer orders for administrative review.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders() {
        return ApiResponseFactory.ok(orderService.getAllOrders());
    }

    @Operation(
            summary = "Get any order by id",
            description = "Returns the full details of a specific order for administrative review.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(orderService.getOrderForAdmin(orderId));
    }
}
