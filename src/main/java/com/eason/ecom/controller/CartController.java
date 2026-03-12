package com.eason.ecom.controller;

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
import com.eason.ecom.dto.CartItemRequest;
import com.eason.ecom.dto.CartResponse;
import com.eason.ecom.dto.UpdateCartItemRequest;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart endpoints backed by Redis")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(
            summary = "Get the current cart",
            description = "Returns the authenticated user's current cart items, total quantity, and total amount.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(authenticatedUser.getId())));
    }

    @Operation(
            summary = "Add an item to the cart",
            description = "Adds a product to the Redis cart. If the product already exists in the cart, the quantity is increased.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item added to cart"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product or quantity"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Validated CartItemRequest request) {
        cartService.addItem(authenticatedUser.getId(), request.productId(), request.quantity());
        return ResponseEntity.ok(ApiResponse.successMessage("Item added to cart"));
    }

    @Operation(
            summary = "Replace an item's quantity",
            description = "Overwrites the quantity of a cart item with a new value after stock validation.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product or quantity"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Product identifier", example = "5")
            @PathVariable @Positive Long productId,
            @RequestBody @Validated UpdateCartItemRequest request) {
        cartService.updateItem(authenticatedUser.getId(), productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.successMessage("Cart item updated"));
    }

    @Operation(
            summary = "Remove an item from the cart",
            description = "Deletes the selected product entry from the authenticated user's Redis cart.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Product identifier", example = "5")
            @PathVariable @Positive Long productId) {
        cartService.removeItem(authenticatedUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.successMessage("Cart item removed"));
    }
}
