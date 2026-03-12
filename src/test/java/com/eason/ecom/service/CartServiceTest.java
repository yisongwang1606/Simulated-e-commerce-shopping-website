package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.entity.Product;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.support.RedisKeys;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getRedis().setCartPrefix("cart:user:");
        RedisKeys redisKeys = new RedisKeys(appProperties);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        cartService = new CartService(productRepository, redisTemplate, redisKeys);
    }

    @Test
    void getCartQuantitiesRemovesCorruptedRedisEntries() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("1", "2");
        entries.put("broken", "3");
        entries.put("4", "0");
        entries.put("5", "NaN");
        when(hashOperations.entries("cart:user:7")).thenReturn(entries);

        Map<Long, Integer> result = cartService.getCartQuantities(7L);

        assertEquals(Map.of(1L, 2), result);
        verify(hashOperations).delete("cart:user:7", "broken");
        verify(hashOperations).delete("cart:user:7", "4");
        verify(hashOperations).delete("cart:user:7", "5");
    }

    @Test
    void addItemRejectsWhenCombinedQuantityExceedsStock() {
        Product product = buildProduct(5L, 3);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(hashOperations.entries("cart:user:7")).thenReturn(Map.of("5", "2"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> cartService.addItem(7L, 5L, 2));

        assertEquals("Requested quantity exceeds available stock", exception.getMessage());
        verify(hashOperations, never()).put(anyString(), any(), any());
    }

    private Product buildProduct(Long id, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("Keyboard");
        product.setCategory("Electronics");
        product.setPrice(BigDecimal.valueOf(89.99));
        product.setStock(stock);
        product.setDescription("Mechanical keyboard");
        return product;
    }
}
