package com.eason.ecom.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Redis redis = new Redis();

    public Jwt getJwt() {
        return jwt;
    }

    public Redis getRedis() {
        return redis;
    }

    public static class Jwt {
        private String secret;
        private long expiration;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }
    }

    public static class Redis {
        private Duration productTtl = Duration.ofMinutes(30);
        private String tokenPrefix;
        private String productPrefix;
        private String cartPrefix;
        private String popularKey;

        public Duration getProductTtl() {
            return productTtl;
        }

        public void setProductTtl(Duration productTtl) {
            this.productTtl = productTtl;
        }

        public String getTokenPrefix() {
            return tokenPrefix;
        }

        public void setTokenPrefix(String tokenPrefix) {
            this.tokenPrefix = tokenPrefix;
        }

        public String getProductPrefix() {
            return productPrefix;
        }

        public void setProductPrefix(String productPrefix) {
            this.productPrefix = productPrefix;
        }

        public String getCartPrefix() {
            return cartPrefix;
        }

        public void setCartPrefix(String cartPrefix) {
            this.cartPrefix = cartPrefix;
        }

        public String getPopularKey() {
            return popularKey;
        }

        public void setPopularKey(String popularKey) {
            this.popularKey = popularKey;
        }
    }
}
