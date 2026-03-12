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

@Validated
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(authenticatedUser.getId())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Validated CartItemRequest request) {
        cartService.addItem(authenticatedUser.getId(), request.productId(), request.quantity());
        return ResponseEntity.ok(ApiResponse.successMessage("Item added to cart"));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long productId,
            @RequestBody @Validated UpdateCartItemRequest request) {
        cartService.updateItem(authenticatedUser.getId(), productId, request.quantity());
        return ResponseEntity.ok(ApiResponse.successMessage("Cart item updated"));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long productId) {
        cartService.removeItem(authenticatedUser.getId(), productId);
        return ResponseEntity.ok(ApiResponse.successMessage("Cart item removed"));
    }
}
