package com.eason.ecom.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.CartItemResponse;
import com.eason.ecom.dto.CartResponse;
import com.eason.ecom.entity.Product;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.ProductRepository;

@Service
public class CartService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public CartService(
            ProductRepository productRepository,
            StringRedisTemplate redisTemplate,
            AppProperties appProperties) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Map<Long, Integer> quantities = getCartQuantities(userId);
        if (quantities.isEmpty()) {
            return new CartResponse(List.of(), 0, BigDecimal.ZERO);
        }

        Map<Long, Product> products = productRepository.findAllById(quantities.keySet()).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

        List<CartItemResponse> items = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) {
                removeItem(userId, entry.getKey());
                continue;
            }
            int quantity = entry.getValue();
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            items.add(new CartItemResponse(
                    product.getId(),
                    product.getName(),
                    product.getCategory(),
                    product.getPrice(),
                    quantity,
                    subtotal));
            totalQuantity += quantity;
            totalPrice = totalPrice.add(subtotal);
        }

        return new CartResponse(items, totalQuantity, totalPrice);
    }

    public void addItem(Long userId, Long productId, Integer quantity) {
        Product product = getExistingProduct(productId);
        int currentQuantity = getCartQuantities(userId).getOrDefault(productId, 0);
        int newQuantity = currentQuantity + quantity;
        validateRequestedQuantity(product, newQuantity);
        putQuantity(userId, productId, newQuantity);
    }

    public void updateItem(Long userId, Long productId, Integer quantity) {
        Product product = getExistingProduct(productId);
        validateRequestedQuantity(product, quantity);
        putQuantity(userId, productId, quantity);
    }

    public void removeItem(Long userId, Long productId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(productId));
    }

    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
    }

    public Map<Long, Integer> getCartQuantities(Long userId) {
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        Map<Object, Object> entries = hashOperations.entries(cartKey(userId));
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                long productId = Long.parseLong(entry.getKey().toString());
                int quantity = Integer.parseInt(entry.getValue().toString());
                if (quantity <= 0) {
                    hashOperations.delete(cartKey(userId), entry.getKey());
                    continue;
                }
                result.put(productId, quantity);
            } catch (NumberFormatException exception) {
                hashOperations.delete(cartKey(userId), entry.getKey());
            }
        }
        return result;
    }

    private Product getExistingProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BadRequestException("Product does not exist"));
    }

    private void validateRequestedQuantity(Product product, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }
        if (quantity > product.getStock()) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }
    }

    private void putQuantity(Long userId, Long productId, int quantity) {
        redisTemplate.opsForHash().put(cartKey(userId), String.valueOf(productId), String.valueOf(quantity));
    }

    private String cartKey(Long userId) {
        return appProperties.getRedis().getCartPrefix() + userId;
    }
}
