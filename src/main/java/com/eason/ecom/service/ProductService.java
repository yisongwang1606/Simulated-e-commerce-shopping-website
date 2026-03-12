package com.eason.ecom.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.PopularProductResponse;
import com.eason.ecom.dto.ProductRequest;
import com.eason.ecom.dto.ProductResponse;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.ProductStatus;
import com.eason.ecom.entity.TaxClass;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.support.RedisKeys;
import jakarta.persistence.criteria.Predicate;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final RedisKeys redisKeys;

    public ProductService(
            ProductRepository productRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties,
            RedisKeys redisKeys) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.redisKeys = redisKeys;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProducts(String keyword, String category, int page, int size) {
        Page<Product> productPage = productRepository.findAll(buildSpecification(keyword, category),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));

        List<ProductResponse> items = productPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(
                items,
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.getNumber(),
                productPage.getSize());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        String cacheKey = redisKeys.product(productId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cached)) {
            try {
                ProductResponse response = objectMapper.readValue(cached, ProductResponse.class);
                trackProductView(productId);
                return response;
            } catch (Exception ignored) {
                redisTemplate.delete(cacheKey);
            }
        }

        Product product = findActiveProductEntity(productId);
        ProductResponse response = toResponse(product);
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(response),
                    appProperties.getRedis().getProductTtl());
        } catch (Exception ignored) {
            // Cache failure should not break the main flow.
        }
        trackProductView(productId);
        return response;
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    @Transactional(readOnly = true)
    public List<PopularProductResponse> getPopularProducts(int limit) {
        Set<TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(redisKeys.popularProducts(), 0, Math.max(0, limit - 1));

        if (tuples == null || tuples.isEmpty()) {
            return productRepository.findAll(PageRequest.of(0, limit, Sort.by("id").ascending()))
                    .stream()
                    .map(this::toResponse)
                    .map(product -> new PopularProductResponse(product, 0))
                    .toList();
        }

        Map<Long, Double> scores = new LinkedHashMap<>();
        for (TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() != null) {
                scores.put(Long.parseLong(tuple.getValue()), tuple.getScore() == null ? 0 : tuple.getScore());
            }
        }

        Map<Long, ProductResponse> productMap = productRepository.findAllById(scores.keySet()).stream()
                .map(this::toResponse)
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.id(), item), Map::putAll);

        List<PopularProductResponse> result = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            ProductResponse product = productMap.get(entry.getKey());
            if (product != null) {
                result.add(new PopularProductResponse(product, entry.getValue()));
            }
        }
        return result;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        applyProductRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = findProductEntity(productId);
        applyProductRequest(product, request);
        Product saved = productRepository.save(product);
        evictProductCache(productId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = findProductEntity(productId);
        productRepository.delete(product);
        evictProductCache(productId);
        redisTemplate.opsForZSet().remove(redisKeys.popularProducts(), String.valueOf(productId));
    }

    public void evictProductCaches(Iterable<Long> productIds) {
        productIds.forEach(this::evictProductCache);
    }

    public Product findProductEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Specification<Product> buildSpecification(String keyword, String category) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), ProductStatus.ACTIVE));
            if (StringUtils.hasText(keyword)) {
                String likeValue = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeValue),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeValue)));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        category.trim().toLowerCase()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyProductRequest(Product product, ProductRequest request) {
        product.setSku(resolveSku(request.sku(), product.getSku()));
        product.setName(request.name().trim());
        product.setBrand(resolveBrand(request.brand()));
        product.setPrice(request.price());
        product.setCostPrice(resolveCostPrice(request.costPrice(), request.price()));
        product.setStock(request.stock());
        product.setSafetyStock(resolveSafetyStock(request.safetyStock(), request.stock()));
        product.setCategory(request.category().trim());
        product.setStatus(resolveStatus(request.status()));
        product.setTaxClass(resolveTaxClass(request.taxClass()));
        product.setWeightKg(resolveWeight(request.weightKg()));
        product.setLeadTimeDays(resolveLeadTimeDays(request.leadTimeDays()));
        product.setFeatured(Boolean.TRUE.equals(request.featured()));
        product.setDescription(request.description().trim());
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStock(),
                product.getSafetyStock(),
                product.getCategory(),
                product.getStatus().name(),
                product.getTaxClass().name(),
                product.getWeightKg(),
                product.getLeadTimeDays(),
                Boolean.TRUE.equals(product.getFeatured()),
                product.getDescription(),
                product.getCreatedAt());
    }

    private void evictProductCache(Long productId) {
        redisTemplate.delete(redisKeys.product(productId));
    }

    private void trackProductView(Long productId) {
        redisTemplate.opsForZSet().incrementScore(
                redisKeys.popularProducts(),
                String.valueOf(productId),
                1);
    }

    private Product findActiveProductEntity(Long productId) {
        Product product = findProductEntity(productId);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product not found");
        }
        return product;
    }

    private String resolveSku(String requestedSku, String existingSku) {
        if (StringUtils.hasText(requestedSku)) {
            return requestedSku.trim().toUpperCase();
        }
        if (StringUtils.hasText(existingSku)) {
            return existingSku;
        }
        return "GEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String resolveBrand(String requestedBrand) {
        return StringUtils.hasText(requestedBrand) ? requestedBrand.trim() : "House Brand";
    }

    private java.math.BigDecimal resolveCostPrice(java.math.BigDecimal requestedCostPrice, java.math.BigDecimal salePrice) {
        if (requestedCostPrice != null) {
            return requestedCostPrice;
        }
        return salePrice.multiply(java.math.BigDecimal.valueOf(0.6)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private int resolveSafetyStock(Integer requestedSafetyStock, Integer stock) {
        if (requestedSafetyStock != null) {
            return requestedSafetyStock;
        }
        return Math.max(1, Math.min(stock, 5));
    }

    private ProductStatus resolveStatus(String requestedStatus) {
        if (!StringUtils.hasText(requestedStatus)) {
            return ProductStatus.ACTIVE;
        }
        return ProductStatus.valueOf(requestedStatus.trim().toUpperCase());
    }

    private TaxClass resolveTaxClass(String requestedTaxClass) {
        if (!StringUtils.hasText(requestedTaxClass)) {
            return TaxClass.STANDARD;
        }
        return TaxClass.valueOf(requestedTaxClass.trim().toUpperCase());
    }

    private java.math.BigDecimal resolveWeight(java.math.BigDecimal requestedWeight) {
        return requestedWeight == null ? java.math.BigDecimal.valueOf(0.1) : requestedWeight;
    }

    private int resolveLeadTimeDays(Integer requestedLeadTimeDays) {
        return requestedLeadTimeDays == null ? 3 : requestedLeadTimeDays;
    }
}
