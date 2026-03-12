package com.eason.ecom.support;

import org.springframework.stereotype.Component;

import com.eason.ecom.config.AppProperties;

@Component
public class RedisKeys {

    private final AppProperties appProperties;

    public RedisKeys(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String cart(Long userId) {
        return appProperties.getRedis().getCartPrefix() + userId;
    }

    public String product(Long productId) {
        return appProperties.getRedis().getProductPrefix() + productId;
    }

    public String token(String token) {
        return appProperties.getRedis().getTokenPrefix() + token;
    }

    public String popularProducts() {
        return appProperties.getRedis().getPopularKey();
    }
}
