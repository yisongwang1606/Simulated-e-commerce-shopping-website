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
    private final Kafka kafka = new Kafka();
    private final Integrations integrations = new Integrations();
    private final Stripe stripe = new Stripe();

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

    public Kafka getKafka() {
        return kafka;
    }

    public Integrations getIntegrations() {
        return integrations;
    }

    public Stripe getStripe() {
        return stripe;
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

    public static class Kafka {
        private String orderTopic = "ecom.order.lifecycle.v1";
        private String deadLetterTopic;
        private String consumerGroup = "ecom-order-events";
        private int topicPartitions = 3;
        private short topicReplicas = 1;
        private long consumerRetryBackoffMs = 1_000L;
        private long consumerMaxRetries = 3L;

        public String getOrderTopic() {
            return orderTopic;
        }

        public void setOrderTopic(String orderTopic) {
            this.orderTopic = orderTopic;
        }

        public String getDeadLetterTopic() {
            return deadLetterTopic == null || deadLetterTopic.isBlank()
                    ? orderTopic + ".dlt"
                    : deadLetterTopic;
        }

        public void setDeadLetterTopic(String deadLetterTopic) {
            this.deadLetterTopic = deadLetterTopic;
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = consumerGroup;
        }

        public int getTopicPartitions() {
            return topicPartitions;
        }

        public void setTopicPartitions(int topicPartitions) {
            this.topicPartitions = topicPartitions;
        }

        public short getTopicReplicas() {
            return topicReplicas;
        }

        public void setTopicReplicas(short topicReplicas) {
            this.topicReplicas = topicReplicas;
        }

        public long getConsumerRetryBackoffMs() {
            return consumerRetryBackoffMs;
        }

        public void setConsumerRetryBackoffMs(long consumerRetryBackoffMs) {
            this.consumerRetryBackoffMs = consumerRetryBackoffMs;
        }

        public long getConsumerMaxRetries() {
            return consumerMaxRetries;
        }

        public void setConsumerMaxRetries(long consumerMaxRetries) {
            this.consumerMaxRetries = consumerMaxRetries;
        }
    }

    public static class Stripe {
        private boolean enabled;
        private String secretKey;
        private String webhookSecret;
        private String currency = "cad";
        private String defaultTestPaymentMethod = "pm_card_visa";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getDefaultTestPaymentMethod() {
            return defaultTestPaymentMethod;
        }

        public void setDefaultTestPaymentMethod(String defaultTestPaymentMethod) {
            this.defaultTestPaymentMethod = defaultTestPaymentMethod;
        }
    }
}
