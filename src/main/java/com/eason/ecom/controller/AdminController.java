package com.eason.ecom.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eason.ecom.dto.ApiResponse;
import com.eason.ecom.dto.AuditLogResponse;
import com.eason.ecom.dto.InventoryAdjustmentRequest;
import com.eason.ecom.dto.InventoryAdjustmentResponse;
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.dto.OrderStatusUpdateRequest;
import com.eason.ecom.dto.OrderTagAssignmentRequest;
import com.eason.ecom.dto.OrderTagResponse;
import com.eason.ecom.dto.OrderInternalNoteRequest;
import com.eason.ecom.dto.OrderInternalNoteResponse;
import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.PaymentInitiationRequest;
import com.eason.ecom.dto.PaymentTransactionResponse;
import com.eason.ecom.dto.ProductRequest;
import com.eason.ecom.dto.ProductResponse;
import com.eason.ecom.dto.ShipmentCreateRequest;
import com.eason.ecom.dto.ShipmentResponse;
import com.eason.ecom.dto.ShipmentStatusUpdateRequest;
import com.eason.ecom.dto.RefundRequestResponse;
import com.eason.ecom.dto.RefundSummaryResponse;
import com.eason.ecom.dto.RefundReviewRequest;
import com.eason.ecom.dto.SupportTicketResponse;
import com.eason.ecom.dto.SupportTicketUpdateRequest;
import com.eason.ecom.entity.Product;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.service.AuditLogService;
import com.eason.ecom.service.InventoryService;
import com.eason.ecom.service.OrderService;
import com.eason.ecom.service.OrderTagService;
import com.eason.ecom.service.OrderInternalNoteService;
import com.eason.ecom.service.PaymentService;
import com.eason.ecom.service.ProductService;
import com.eason.ecom.service.RefundService;
import com.eason.ecom.service.ShipmentService;
import com.eason.ecom.service.SupportTicketService;
import com.eason.ecom.support.ApiResponseFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrator-only catalog and order management endpoints")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final PaymentService paymentService;
    private final ShipmentService shipmentService;
    private final OrderInternalNoteService orderInternalNoteService;
    private final RefundService refundService;
    private final OrderTagService orderTagService;
    private final SupportTicketService supportTicketService;

    public AdminController(
            ProductService productService,
            OrderService orderService,
            InventoryService inventoryService,
            AuditLogService auditLogService,
            PaymentService paymentService,
            ShipmentService shipmentService,
            OrderInternalNoteService orderInternalNoteService,
            RefundService refundService,
            OrderTagService orderTagService,
            SupportTicketService supportTicketService) {
        this.productService = productService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.paymentService = paymentService;
        this.shipmentService = shipmentService;
        this.orderInternalNoteService = orderInternalNoteService;
        this.refundService = refundService;
        this.orderTagService = orderTagService;
        this.supportTicketService = supportTicketService;
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody @Validated ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        auditLogService.recordUserAction(
                authenticatedUser.getId(),
                authenticatedUser.getUsername(),
                "PRODUCT_CREATED",
                "PRODUCT",
                String.valueOf(product.id()),
                "Product " + product.sku() + " created",
                java.util.Map.of(
                        "sku", product.sku(),
                        "name", product.name(),
                        "status", product.status()));
        return ApiResponseFactory.created("Product created successfully", product);
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId,
            @RequestBody @Validated ProductRequest request) {
        ProductResponse product = productService.updateProduct(productId, request);
        auditLogService.recordUserAction(
                authenticatedUser.getId(),
                authenticatedUser.getUsername(),
                "PRODUCT_UPDATED",
                "PRODUCT",
                String.valueOf(productId),
                "Product " + product.sku() + " updated",
                java.util.Map.of(
                        "sku", product.sku(),
                        "name", product.name(),
                        "status", product.status()));
        return ApiResponseFactory.ok("Product updated successfully", product);
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId) {
        Product product = productService.findProductEntity(productId);
        productService.deleteProduct(productId);
        auditLogService.recordUserAction(
                authenticatedUser.getId(),
                authenticatedUser.getUsername(),
                "PRODUCT_DELETED",
                "PRODUCT",
                String.valueOf(productId),
                "Product " + product.getSku() + " deleted",
                java.util.Map.of(
                        "sku", product.getSku(),
                        "name", product.getName()));
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
            summary = "Search orders with admin filters",
            description = "Returns paginated orders filtered by status, customer keyword, and created date range.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filtered orders loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/orders/search")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> searchOrders(
            @Parameter(description = "Optional order status filter", example = "PAID")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @Parameter(description = "Optional customer keyword matching order number, username, or email", example = "demo")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String customer,
            @Parameter(description = "Optional inclusive start date", example = "2026-03-01")
            @org.springframework.web.bind.annotation.RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateFrom,
            @Parameter(description = "Optional inclusive end date", example = "2026-03-31")
            @org.springframework.web.bind.annotation.RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateTo,
            @Parameter(description = "Zero-based page index", example = "0")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size", example = "20")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") @Positive int size) {
        return ApiResponseFactory.ok(orderService.searchOrdersForAdmin(status, customer, dateFrom, dateTo, page, size));
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

    @Operation(
            summary = "List reusable order tags",
            description = "Returns the operational tag catalog available for order triage and exception handling.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order tags loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/order-tags")
    public ResponseEntity<ApiResponse<List<OrderTagResponse>>> getOrderTags() {
        return ApiResponseFactory.ok(orderTagService.getOrderTagCatalog());
    }

    @Operation(
            summary = "List tags assigned to an order",
            description = "Returns the operational tags currently assigned to one order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order tags loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/orders/{orderId}/tags")
    public ResponseEntity<ApiResponse<List<OrderTagResponse>>> getOrderTagsForOrder(
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(orderTagService.getOrderTags(orderId));
    }

    @Operation(
            summary = "Assign a tag to an order",
            description = "Adds an operational tag such as VIP, fraud review, or address check to an order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order tag assigned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid tag payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or tag not found")
    })
    @PostMapping("/orders/{orderId}/tags")
    public ResponseEntity<ApiResponse<List<OrderTagResponse>>> assignOrderTag(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @RequestBody @Validated OrderTagAssignmentRequest request) {
        return ApiResponseFactory.ok(
                "Order tag assigned successfully",
                orderTagService.assignTag(
                        orderId,
                        request.orderTagId(),
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "Remove a tag from an order",
            description = "Removes an operational tag from an order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order tag removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or assignment not found")
    })
    @DeleteMapping("/orders/{orderId}/tags/{orderTagId}")
    public ResponseEntity<ApiResponse<List<OrderTagResponse>>> removeOrderTag(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @Parameter(description = "Order tag identifier", example = "1")
            @PathVariable @Positive Long orderTagId) {
        return ApiResponseFactory.ok(
                "Order tag removed successfully",
                orderTagService.removeTag(
                        orderId,
                        orderTagId,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "Move an order to the next lifecycle status",
            description = "Updates an order status using enterprise workflow rules and records an audit trail entry.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition or malformed payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @RequestBody @Validated OrderStatusUpdateRequest request) {
        return ApiResponseFactory.ok(
                "Order status updated successfully",
                orderService.updateOrderStatusForAdmin(
                        orderId,
                        request.status(),
                        request.note(),
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "Create a placeholder payment transaction",
            description = "Registers a simulated payment request and moves the order into PAYMENT_PENDING when appropriate.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment transaction created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid order or payment payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<PaymentTransactionResponse>> createPayment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @RequestBody @Validated PaymentInitiationRequest request) {
        return ApiResponseFactory.created(
                "Payment transaction created successfully",
                paymentService.initiatePayment(
                        orderId,
                        request,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "List payment transactions for an order",
            description = "Returns simulated payment transactions for administrative review.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment transactions loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/orders/{orderId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentTransactionResponse>>> getPayments(
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(paymentService.getPaymentsForOrder(orderId));
    }

    @Operation(
            summary = "Create a shipment placeholder for a paid order",
            description = "Creates a shipment record, advances the order to ALLOCATED/SHIPPED, and stores tracking details.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Shipment created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order not ready for shipment or payload invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @PostMapping("/orders/{orderId}/shipments")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @RequestBody @Validated ShipmentCreateRequest request) {
        return ApiResponseFactory.created(
                "Shipment created successfully",
                shipmentService.createShipment(
                        orderId,
                        request,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "List shipments for an order",
            description = "Returns shipment placeholders and tracking details for an order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipments loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/orders/{orderId}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponse>>> getShipments(
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(shipmentService.getShipmentsForOrder(orderId));
    }

    @Operation(
            summary = "Add an internal note to an order",
            description = "Stores an admin-only operational note for follow-up, support, or warehouse coordination.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Internal note created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid note payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PostMapping("/orders/{orderId}/notes")
    public ResponseEntity<ApiResponse<OrderInternalNoteResponse>> addOrderNote(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId,
            @RequestBody @Validated OrderInternalNoteRequest request) {
        return ApiResponseFactory.created(
                "Order internal note created successfully",
                orderInternalNoteService.addNote(
                        orderId,
                        request,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "List internal notes for an order",
            description = "Returns admin-only operational notes attached to an order.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order notes loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/orders/{orderId}/notes")
    public ResponseEntity<ApiResponse<List<OrderInternalNoteResponse>>> getOrderNotes(
            @Parameter(description = "Order identifier", example = "1")
            @PathVariable @Positive Long orderId) {
        return ApiResponseFactory.ok(orderInternalNoteService.getNotes(orderId));
    }

    @Operation(
            summary = "List refund requests",
            description = "Returns refund requests for review, optionally filtered by refund status.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund requests loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/refund-requests")
    public ResponseEntity<ApiResponse<PagedResponse<RefundRequestResponse>>> getRefundRequests(
            @Parameter(description = "Optional refund status filter", example = "REQUESTED")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @Parameter(description = "Zero-based page index", example = "0")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size", example = "20")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") @Positive int size) {
        return ApiResponseFactory.ok(refundService.getRefundRequestsForAdmin(status, page, size));
    }

    @Operation(
            summary = "Get refund summary metrics",
            description = "Returns aggregated refund counts and amounts for dashboard use, optionally filtered by requested date range.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund summary loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/refund-requests/summary")
    public ResponseEntity<ApiResponse<RefundSummaryResponse>> getRefundSummary(
            @Parameter(description = "Optional inclusive start date", example = "2026-03-01")
            @org.springframework.web.bind.annotation.RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateFrom,
            @Parameter(description = "Optional inclusive end date", example = "2026-03-31")
            @org.springframework.web.bind.annotation.RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate dateTo) {
        return ApiResponseFactory.ok(refundService.getRefundSummary(dateFrom, dateTo));
    }

    @Operation(
            summary = "Review a refund request",
            description = "Approves or rejects a refund request and moves the order into REFUND_PENDING when approved.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund request reviewed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid decision or request already reviewed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Refund request not found")
    })
    @PutMapping("/refund-requests/{refundRequestId}/review")
    public ResponseEntity<ApiResponse<RefundRequestResponse>> reviewRefundRequest(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Refund request identifier", example = "1")
            @PathVariable @Positive Long refundRequestId,
            @RequestBody @Validated RefundReviewRequest request) {
        return ApiResponseFactory.ok(
                "Refund request reviewed successfully",
                refundService.reviewRefundRequest(
                        refundRequestId,
                        request,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "Mark a shipment as delivered",
            description = "Marks a shipment as delivered and advances the order to COMPLETED when applicable.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shipment updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid shipment transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    @PutMapping("/shipments/{shipmentId}/deliver")
    public ResponseEntity<ApiResponse<ShipmentResponse>> markShipmentDelivered(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Shipment identifier", example = "1")
            @PathVariable @Positive Long shipmentId,
            @RequestBody(required = false) ShipmentStatusUpdateRequest request) {
        return ApiResponseFactory.ok(
                "Shipment marked as delivered",
                shipmentService.markDelivered(
                        shipmentId,
                        request == null ? null : request.note(),
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "List customer support tickets",
            description = "Returns support tickets with admin filters such as status, priority, and assigned team.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Support tickets loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/support-tickets")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicketResponse>>> getSupportTickets(
            @Parameter(description = "Optional support ticket status filter", example = "OPEN")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String status,
            @Parameter(description = "Optional support ticket priority filter", example = "HIGH")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String priority,
            @Parameter(description = "Optional assigned team filter", example = "Customer Support")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String assignedTeam,
            @Parameter(description = "Zero-based page index", example = "0")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size", example = "20")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") @Positive int size) {
        return ApiResponseFactory.ok(supportTicketService.getTicketsForAdmin(status, priority, assignedTeam, page, size));
    }

    @Operation(
            summary = "Update a support ticket",
            description = "Updates a support ticket status, assignment, and notes from the admin service desk.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Support ticket updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid support ticket payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Support ticket not found")
    })
    @PutMapping("/support-tickets/{ticketId}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateSupportTicket(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Support ticket identifier", example = "1")
            @PathVariable @Positive Long ticketId,
            @RequestBody @Validated SupportTicketUpdateRequest request) {
        return ApiResponseFactory.ok(
                "Support ticket updated successfully",
                supportTicketService.updateTicket(
                        ticketId,
                        request,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "Create a manual inventory adjustment",
            description = "Applies a warehouse stock correction, increase, or decrease and stores an inventory movement record.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Inventory adjustment created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid adjustment payload"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PostMapping("/products/{productId}/inventory-adjustments")
    public ResponseEntity<ApiResponse<InventoryAdjustmentResponse>> adjustInventory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId,
            @RequestBody @Validated InventoryAdjustmentRequest request) {
        return ApiResponseFactory.created(
                "Inventory adjustment created successfully",
                inventoryService.adjustStock(
                        productId,
                        request,
                        authenticatedUser.getId(),
                        authenticatedUser.getUsername()));
    }

    @Operation(
            summary = "List inventory adjustments for a product",
            description = "Returns the most recent stock movement records for one product.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inventory adjustments loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/products/{productId}/inventory-adjustments")
    public ResponseEntity<ApiResponse<List<InventoryAdjustmentResponse>>> getInventoryAdjustments(
            @Parameter(description = "Product identifier", example = "1")
            @PathVariable @Positive Long productId) {
        return ApiResponseFactory.ok(inventoryService.getAdjustmentsForProduct(productId));
    }

    @Operation(
            summary = "Query audit logs",
            description = "Returns the latest audit trail entries with optional entity filters for operational investigations.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit logs loaded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAuditLogs(
            @Parameter(description = "Optional entity type filter", example = "ORDER")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String entityType,
            @Parameter(description = "Optional entity id filter", example = "ORD-202603120001-1001")
            @org.springframework.web.bind.annotation.RequestParam(required = false) String entityId,
            @Parameter(description = "Maximum number of audit records to return", example = "50")
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") @PositiveOrZero int limit) {
        return ApiResponseFactory.ok(auditLogService.getAuditTrail(entityType, entityId, limit));
    }
}
