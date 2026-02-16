package com.ksa.financing.infra.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisIdempotencyStoreTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.0"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private IdempotencyStore idempotencyStore;

    @Test
    void shouldExecuteOperationOnce() {
        AtomicInteger counter = new AtomicInteger(0);
        String key = "test-key-1";

        String result1 = idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        });

        String result2 = idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        });

        assertThat(result1).isEqualTo("result");
        assertThat(result2).isEqualTo("result");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldExpireAfterTTL() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        String key = "test-key-2";

        idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        }, Duration.ofSeconds(1));

        Thread.sleep(1100);

        idempotencyStore.executeIdempotent(key, () -> {
            counter.incrementAndGet();
            return "result";
        }, Duration.ofSeconds(1));

        assertThat(counter.get()).isEqualTo(2);
    }
}
