package com.eason.ecom.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Order order = new Order();
    private final Seed seed = new Seed();
    private final Redis redis = new Redis();
    private final Integrations integrations = new Integrations();

    public Jwt getJwt() {
        return jwt;
    }

    public Order getOrder() {
        return order;
    }

    public Seed getSeed() {
        return seed;
    }

    public Redis getRedis() {
        return redis;
    }

    public Integrations getIntegrations() {
        return integrations;
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

    public static class Order {
        private java.math.BigDecimal defaultTaxRate = java.math.BigDecimal.ZERO;
        private java.math.BigDecimal defaultShippingFee = java.math.BigDecimal.ZERO;

        public java.math.BigDecimal getDefaultTaxRate() {
            return defaultTaxRate;
        }

        public void setDefaultTaxRate(java.math.BigDecimal defaultTaxRate) {
            this.defaultTaxRate = defaultTaxRate;
        }

        public java.math.BigDecimal getDefaultShippingFee() {
            return defaultShippingFee;
        }

        public void setDefaultShippingFee(java.math.BigDecimal defaultShippingFee) {
            this.defaultShippingFee = defaultShippingFee;
        }
    }

    public static class Seed {
        private String productMasterResource = "seed-data/product-master-100.csv";

        public String getProductMasterResource() {
            return productMasterResource;
        }

        public void setProductMasterResource(String productMasterResource) {
            this.productMasterResource = productMasterResource;
        }
    }

    public static class Integrations {
        private String paymentCallbackToken = "local-payment-callback-token";

        public String getPaymentCallbackToken() {
            return paymentCallbackToken;
        }

        public void setPaymentCallbackToken(String paymentCallbackToken) {
            this.paymentCallbackToken = paymentCallbackToken;
        }
    }
}
