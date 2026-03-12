package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.InventoryAdjustmentRequest;
import com.eason.ecom.dto.InventoryAdjustmentResponse;
import com.eason.ecom.entity.InventoryAdjustment;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.ProductStatus;
import com.eason.ecom.entity.TaxClass;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.InventoryAdjustmentRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.support.RedisKeys;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private AuditLogService auditLogService;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getRedis().setProductPrefix("product:detail:");
        appProperties.getRedis().setCartPrefix("cart:");
        appProperties.getRedis().setTokenPrefix("auth:token:");
        appProperties.getRedis().setPopularKey("product:popular");
        appProperties.getRedis().setProductTtl(Duration.ofMinutes(30));

        inventoryService = new InventoryService(
                inventoryAdjustmentRepository,
                productRepository,
                redisTemplate,
                new RedisKeys(appProperties),
                auditLogService);
    }

    @Test
    void increaseAdjustmentUpdatesStockAndWritesAudit() {
        Product product = buildProduct(1L, 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class))).thenAnswer(invocation -> {
            InventoryAdjustment adjustment = invocation.getArgument(0);
            adjustment.setId(99L);
            adjustment.setCreatedAt(LocalDateTime.of(2026, 3, 12, 13, 0));
            return adjustment;
        });

        InventoryAdjustmentResponse response = inventoryService.adjustStock(
                1L,
                new InventoryAdjustmentRequest("INCREASE", 5, "Supplier replenishment received", "PURCHASE_ORDER", "PO-20260312-01"),
                8L,
                "warehouse.admin");

        assertEquals(15, product.getStock());
        assertEquals("INCREASE", response.adjustmentType());
        assertEquals(10, response.previousStock());
        assertEquals(15, response.newStock());
        verify(redisTemplate).delete("product:detail:1");
        verify(auditLogService).recordUserAction(eq(8L), eq("warehouse.admin"), eq("INVENTORY_ADJUSTED"), eq("PRODUCT"), eq("1"), any(), any());
    }

    @Test
    void decreaseAdjustmentRejectsNegativeResultingStock() {
        Product product = buildProduct(1L, 3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> inventoryService.adjustStock(
                        1L,
                        new InventoryAdjustmentRequest("DECREASE", 5, "Damaged stock write-off", "QUALITY", "QA-20260312-09"),
                        8L,
                        "warehouse.admin"));

        assertEquals("Inventory adjustment would make stock negative", exception.getMessage());
        verify(inventoryAdjustmentRepository, never()).save(any(InventoryAdjustment.class));
        verify(auditLogService, never()).recordUserAction(any(), any(), any(), any(), any(), any(), any());
    }

    private Product buildProduct(Long id, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setSku("ELE-000" + id);
        product.setName("Warehouse Keyboard");
        product.setBrand("NorthPeak Tech");
        product.setCategory("Electronics");
        product.setDescription("Warehouse Keyboard description");
        product.setPrice(BigDecimal.valueOf(129.99));
        product.setCostPrice(BigDecimal.valueOf(79.99));
        product.setStock(stock);
        product.setSafetyStock(4);
        product.setTaxClass(TaxClass.STANDARD);
        product.setStatus(ProductStatus.ACTIVE);
        product.setWeightKg(BigDecimal.valueOf(0.5));
        product.setLeadTimeDays(3);
        product.setFeatured(false);
        return product;
    }
}
