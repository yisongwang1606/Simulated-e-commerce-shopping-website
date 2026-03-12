package com.eason.ecom.security;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.eason.ecom.support.RedisKeys;

@Service
public class TokenStoreService {

    private final StringRedisTemplate redisTemplate;
    private final RedisKeys redisKeys;

    public TokenStoreService(StringRedisTemplate redisTemplate, RedisKeys redisKeys) {
        this.redisTemplate = redisTemplate;
        this.redisKeys = redisKeys;
    }

    public void store(String token, Duration ttl) {
        redisTemplate.opsForValue().set(redisKeys.token(token), "1", ttl);
    }

    public boolean isActive(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKeys.token(token)));
    }

    public void revoke(String token) {
        redisTemplate.delete(redisKeys.token(token));
    }
}
