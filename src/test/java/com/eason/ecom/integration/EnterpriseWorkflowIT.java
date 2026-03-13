package com.eason.ecom.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.messaging.OrderEventType;
import com.eason.ecom.messaging.OrderLifecycleEvent;
import com.eason.ecom.repository.OrderEventReceiptRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import tools.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class EnterpriseWorkflowIT {

    private static final String REDIS_PASSWORD = "test-redis-pass";
    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.4-alpine");
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:3.8.0");
    private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:4.1.8-management");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("ecom_enterprise")
            .withUsername("ecom")
            .withPassword("ecom");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE)
            .withCommand("redis-server", "--save", "", "--appendonly", "no", "--requirepass", REDIS_PASSWORD)
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(RABBITMQ_IMAGE);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.rabbitmq.enabled", () -> true);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("app.stripe.enabled", () -> false);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OrderEventReceiptRepository orderEventReceiptRepository;

    @Autowired
    private KafkaTemplate<String, OrderLifecycleEvent> orderEventKafkaTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AppProperties appProperties;

    @Test
    void createsOrderAndPersistsKafkaAndRabbitReceipts() {
        String token = loginAndGetToken("demo@ecom.local", "Demo123!");
        HttpHeaders headers = bearerHeaders(token);

        ResponseEntity<JsonNode> addToCartResponse = restClient().post()
                .uri("/api/cart/items")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(Map.of("productId", 1, "quantity", 1))
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(addToCartResponse.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<JsonNode> orderResponse = restClient().post()
                .uri("/api/orders")
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(Map.of())
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(orderResponse.getStatusCode().value()).isEqualTo(201);
        JsonNode orderData = orderResponse.getBody().path("data");
        String orderNo = orderData.path("orderNo").asText();
        assertThat(orderNo).isNotBlank();

        Awaitility.await()
                .atMost(Duration.ofSeconds(25))
                .untilAsserted(() -> {
                    assertThat(orderEventReceiptRepository.countByOrderNo(orderNo)).isGreaterThanOrEqualTo(2);
                    assertThat(orderEventReceiptRepository.countByOrderNoAndConsumerGroup(orderNo, "ecom-order-events"))
                            .isEqualTo(1);
                    assertThat(orderEventReceiptRepository.countByOrderNoAndConsumerGroup(orderNo, "ecom-rabbit-order-events"))
                            .isEqualTo(1);
                });

        ResponseEntity<JsonNode> readinessResponse = restClient().get()
                .uri("/actuator/health/readiness")
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(readinessResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(readinessResponse.getBody().path("status").asText()).isEqualTo("UP");
    }

    @Test
    void routesInvalidMessagesToKafkaDltAndRabbitDlq() throws Exception {
        OrderLifecycleEvent invalidEvent = new OrderLifecycleEvent(
                UUID.randomUUID().toString(),
                OrderEventType.ORDER_CREATED,
                9999L,
                null,
                "CREATED",
                "integration-test",
                "integration-test",
                Instant.now(),
                Map.of("scenario", "dead-letter"));

        orderEventKafkaTemplate.send(kafkaOrderTopic(), "dead-letter-order", invalidEvent)
                .get(10, TimeUnit.SECONDS);
        orderEventKafkaTemplate.flush();
        rabbitTemplate.convertAndSend(rabbitExchange(), rabbitRoutingKey(), invalidEvent);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    assertThat(readKafkaTopicMessageCount(appProperties.getKafka().getDeadLetterTopic()))
                            .isGreaterThanOrEqualTo(1L);
                    assertThat(readRabbitQueueMessageCount(appProperties.getRabbitmq().getDeadLetterQueue()))
                            .isGreaterThanOrEqualTo(1L);
                });
    }

    private String loginAndGetToken(String username, String password) {
        ResponseEntity<JsonNode> loginResponse = restClient().post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .retrieve()
                .toEntity(JsonNode.class);
        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();
        return loginResponse.getBody().path("data").path("token").asText();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    private String kafkaOrderTopic() {
        return appProperties.getKafka().getOrderTopic();
    }

    private String rabbitExchange() {
        return appProperties.getRabbitmq().getExchange();
    }

    private String rabbitRoutingKey() {
        return appProperties.getRabbitmq().getRoutingKey();
    }

    private long readKafkaTopicMessageCount(String topic) throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers()))) {
            Map<TopicPartition, OffsetSpec> offsetRequests = new java.util.HashMap<>();
            adminClient.describeTopics(List.of(topic)).allTopicNames().get(10, TimeUnit.SECONDS)
                    .get(topic)
                    .partitions()
                    .forEach(partition -> offsetRequests.put(
                            new TopicPartition(topic, partition.partition()),
                            OffsetSpec.latest()));
            return adminClient.listOffsets(offsetRequests)
                    .all()
                    .get(10, TimeUnit.SECONDS)
                    .values()
                    .stream()
                    .mapToLong(result -> result.offset())
                    .sum();
        }
    }

    private long readRabbitQueueMessageCount(String queueName) {
        Number messageCount = rabbitTemplate.execute(
                channel -> Integer.valueOf(channel.queueDeclarePassive(queueName).getMessageCount()));
        return messageCount == null ? 0L : messageCount.longValue();
    }
}
