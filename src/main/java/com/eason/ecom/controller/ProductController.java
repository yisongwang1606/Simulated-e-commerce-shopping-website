package com.eason.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.PopularProductResponse;
import com.eason.ecom.dto.ProductResponse;
import com.eason.ecom.service.ProductService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Public product catalog endpoints for browsing, filtering, and analytics")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
            summary = "List products",
            description = "Returns a paginated product catalog. Supports keyword search and category filtering.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product page loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid paging or filter parameters")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @Parameter(description = "Optional keyword applied to product name and description", example = "keyboard")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Optional exact category filter", example = "Electronics")
            @RequestParam(required = false) String category,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size between 1 and 50", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponseFactory.ok(productService.getProducts(keyword, category, page, size));
    }

    @Operation(
            summary = "List available categories",
            description = "Returns all distinct product categories used by the catalog.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories loaded")
    })
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ApiResponseFactory.ok(productService.getCategories());
    }

    @Operation(
            summary = "Get popular products",
            description = "Returns the top viewed products ranked from a Redis sorted set.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Popular products loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid popularity limit")
    })
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PopularProductResponse>>> getPopularProducts(
            @Parameter(description = "Maximum number of products to return", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit) {
        return ApiResponseFactory.ok(productService.getPopularProducts(limit));
    }

    @Operation(
            summary = "Get product details",
            description = "Returns the details for a single product and increments its popularity score.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId) {
        return ApiResponseFactory.ok(productService.getProductById(productId));
    }
}
