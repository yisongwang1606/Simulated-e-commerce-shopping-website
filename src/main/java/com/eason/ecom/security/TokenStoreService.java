package com.eason.ecom.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.eason.ecom.config.AppProperties;

@Service
public class TokenStoreService {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public TokenStoreService(StringRedisTemplate redisTemplate, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    public void store(String token, Duration ttl) {
        redisTemplate.opsForValue().set(tokenKey(token), "1", ttl);
    }

    public boolean isActive(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey(token)));
    }

    public void revoke(String token) {
        redisTemplate.delete(tokenKey(token));
    }

    private String tokenKey(String token) {
        return appProperties.getRedis().getTokenPrefix() + token;
    }
}
