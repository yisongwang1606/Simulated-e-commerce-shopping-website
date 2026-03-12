package com.eason.ecom.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.InventoryAdjustmentRequest;
import com.eason.ecom.dto.InventoryAdjustmentResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.InventoryAdjustment;
import com.eason.ecom.entity.InventoryAdjustmentType;
import com.eason.ecom.entity.OrderItem;
import com.eason.ecom.entity.Product;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.InventoryAdjustmentRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.support.RedisKeys;

@Service
public class InventoryService {

    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisKeys redisKeys;
    private final AuditLogService auditLogService;

    public InventoryService(
            InventoryAdjustmentRepository inventoryAdjustmentRepository,
            ProductRepository productRepository,
            StringRedisTemplate redisTemplate,
            RedisKeys redisKeys,
            AuditLogService auditLogService) {
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.redisKeys = redisKeys;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public InventoryAdjustmentResponse adjustStock(
            Long productId,
            InventoryAdjustmentRequest request,
            Long actorUserId,
            String actorUsername) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        InventoryAdjustmentType adjustmentType = resolveManualAdjustmentType(request.adjustmentType());
        int delta = normalizeManualDelta(adjustmentType, request.quantityDelta());
        InventoryAdjustment adjustment = applyStockChange(
                product,
                adjustmentType,
                delta,
                request.reason(),
                request.referenceType(),
                request.referenceId(),
                actorUserId,
                actorUsername);

        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "INVENTORY_ADJUSTED",
                "PRODUCT",
                String.valueOf(productId),
                "Inventory adjusted for product " + product.getSku(),
                Map.of(
                        "adjustmentType", adjustmentType.name(),
                        "quantityDelta", adjustment.getQuantityDelta(),
                        "previousStock", adjustment.getPreviousStock(),
                        "newStock", adjustment.getNewStock(),
                        "reason", adjustment.getReason()));

        return toResponse(adjustment);
    }

    @Transactional
    public void recordOrderReservation(CustomerOrder customerOrder) {
        for (OrderItem item : customerOrder.getItems()) {
            Product product = item.getProduct();
            int previousStock = product.getStock() + item.getQuantity();
            createAdjustment(
                    product,
                    InventoryAdjustmentType.ORDER_RESERVATION,
                    -item.getQuantity(),
                    previousStock,
                    product.getStock(),
                    "Stock reserved for order " + customerOrder.getOrderNo(),
                    "ORDER",
                    customerOrder.getOrderNo(),
                    customerOrder.getUser().getId(),
                    customerOrder.getUser().getUsername());
        }
    }

    @Transactional
    public void releaseOrderReservation(
            CustomerOrder customerOrder,
            Long actorUserId,
            String actorUsername,
            String note) {
        for (OrderItem item : customerOrder.getItems()) {
            Product product = item.getProduct();
            int previousStock = product.getStock();
            int newStock = previousStock + item.getQuantity();
            product.setStock(newStock);
            createAdjustment(
                    product,
                    InventoryAdjustmentType.ORDER_RELEASE,
                    item.getQuantity(),
                    previousStock,
                    newStock,
                    buildReleaseReason(customerOrder.getOrderNo(), note),
                    "ORDER",
                    customerOrder.getOrderNo(),
                    actorUserId,
                    actorUsername);
            evictProductCache(product.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<InventoryAdjustmentResponse> getAdjustmentsForProduct(Long productId) {
        return inventoryAdjustmentRepository.findTop100ByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    private InventoryAdjustment applyStockChange(
            Product product,
            InventoryAdjustmentType adjustmentType,
            int delta,
            String reason,
            String referenceType,
            String referenceId,
            Long actorUserId,
            String actorUsername) {
        int previousStock = product.getStock();
        int newStock = previousStock + delta;
        if (newStock < 0) {
            throw new BadRequestException("Inventory adjustment would make stock negative");
        }
        product.setStock(newStock);
        evictProductCache(product.getId());
        return createAdjustment(
                product,
                adjustmentType,
                delta,
                previousStock,
                newStock,
                reason,
                referenceType,
                referenceId,
                actorUserId,
                actorUsername);
    }

    private InventoryAdjustment createAdjustment(
            Product product,
            InventoryAdjustmentType adjustmentType,
            int quantityDelta,
            int previousStock,
            int newStock,
            String reason,
            String referenceType,
            String referenceId,
            Long actorUserId,
            String actorUsername) {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setProduct(product);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setQuantityDelta(quantityDelta);
        adjustment.setPreviousStock(previousStock);
        adjustment.setNewStock(newStock);
        adjustment.setReason(reason.trim());
        adjustment.setReferenceType(normalizeOptional(referenceType));
        adjustment.setReferenceId(normalizeOptional(referenceId));
        adjustment.setCreatedByUserId(actorUserId);
        adjustment.setCreatedByUsername(normalizeOptional(actorUsername));
        return inventoryAdjustmentRepository.save(adjustment);
    }

    private InventoryAdjustmentType resolveManualAdjustmentType(String adjustmentType) {
        if (!StringUtils.hasText(adjustmentType)) {
            throw new BadRequestException("adjustmentType must not be blank");
        }
        InventoryAdjustmentType resolved;
        try {
            resolved = InventoryAdjustmentType.valueOf(adjustmentType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported inventory adjustment type: " + adjustmentType);
        }
        if (resolved == InventoryAdjustmentType.ORDER_RESERVATION || resolved == InventoryAdjustmentType.ORDER_RELEASE) {
            throw new BadRequestException("Manual inventory adjustments only support INCREASE, DECREASE, or CORRECTION");
        }
        return resolved;
    }

    private int normalizeManualDelta(InventoryAdjustmentType adjustmentType, Integer quantityDelta) {
        if (quantityDelta == null || quantityDelta == 0) {
            throw new BadRequestException("quantityDelta must not be zero");
        }
        return switch (adjustmentType) {
            case INCREASE -> Math.abs(quantityDelta);
            case DECREASE -> -Math.abs(quantityDelta);
            case CORRECTION -> quantityDelta;
            default -> throw new BadRequestException("Unsupported manual adjustment type");
        };
    }

    private InventoryAdjustmentResponse toResponse(InventoryAdjustment adjustment) {
        return new InventoryAdjustmentResponse(
                adjustment.getId(),
                adjustment.getProduct().getId(),
                adjustment.getProduct().getSku(),
                adjustment.getProduct().getName(),
                adjustment.getAdjustmentType().name(),
                adjustment.getQuantityDelta(),
                adjustment.getPreviousStock(),
                adjustment.getNewStock(),
                adjustment.getReason(),
                adjustment.getReferenceType(),
                adjustment.getReferenceId(),
                adjustment.getCreatedByUsername(),
                adjustment.getCreatedAt());
    }

    private String buildReleaseReason(String orderNo, String note) {
        if (!StringUtils.hasText(note)) {
            return "Stock released after cancelling order " + orderNo;
        }
        return "Stock released after cancelling order " + orderNo + ": " + note.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void evictProductCache(Long productId) {
        redisTemplate.delete(redisKeys.product(productId));
    }
}
