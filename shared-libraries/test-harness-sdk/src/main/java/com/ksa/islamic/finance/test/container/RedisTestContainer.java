package com.ksa.islamic.finance.test.container;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis test container for caching and session testing
 */
@Slf4j
public class RedisTestContainer {
    private static final String REDIS_IMAGE = "redis:7-alpine";
    private static final int REDIS_PORT = 6379;
    private static final Map<String, GenericContainer<?>> containers = new ConcurrentHashMap<>();

    private RedisTestContainer() {}

    /**
     * Get or create a Redis container with default settings
     */
    public static GenericContainer<?> getInstance() {
        return getInstance("test");
    }

    /**
     * Get or create a named Redis container
     */
    public static GenericContainer<?> getInstance(String name) {
        return containers.computeIfAbsent(name, k -> createContainer());
    }

    /**
     * Create a new Redis container
     */
    public static GenericContainer<?> createContainer() {
        GenericContainer<?> container = new GenericContainer<>(
            DockerImageName.parse(REDIS_IMAGE)
        )
            .withExposedPorts(REDIS_PORT)
            .withCommand(
                "redis-server",
                "--maxmemory", "256mb",
                "--maxmemory-policy", "allkeys-lru",
                "--appendonly", "no",
                "--save", ""
            )
            .withStartupTimeout(Duration.ofMinutes(2))
            .withReuse(true);

        container.start();
        initializeTestData(container);

        log.info("Redis container started on port: {}", container.getMappedPort(REDIS_PORT));
        return container;
    }

    /**
     * Create a Jedis pool for the container
     */
    public static JedisPool createJedisPool(GenericContainer<?> container) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        return new JedisPool(
            poolConfig,
            container.getHost(),
            container.getMappedPort(REDIS_PORT)
        );
    }

    /**
     * Get Redis connection string
     */
    public static String getConnectionString(GenericContainer<?> container) {
        return String.format("redis://%s:%d",
            container.getHost(),
            container.getMappedPort(REDIS_PORT)
        );
    }

    /**
     * Initialize test data
     */
    private static void initializeTestData(GenericContainer<?> container) {
        try (JedisPool pool = createJedisPool(container);
             Jedis jedis = pool.getResource()) {

            // Set up test namespaces
            jedis.set("test:namespace:cache", "enabled");
            jedis.set("test:namespace:session", "enabled");
            jedis.set("test:namespace:rate-limit", "enabled");

            // Configure test cache TTLs
            jedis.setex("cache:config:default-ttl", 3600, "300");
            jedis.setex("cache:config:max-ttl", 3600, "3600");

            // Set up test rate limits
            jedis.hset("rate-limit:config", "default", "100");
            jedis.hset("rate-limit:config", "api", "1000");
            jedis.hset("rate-limit:config", "auth", "10");

            log.debug("Redis test data initialized");
        } catch (Exception e) {
            log.error("Failed to initialize test data", e);
        }
    }

    /**
     * Flush all data in the container
     */
    public static void flushAll(GenericContainer<?> container) {
        try (JedisPool pool = createJedisPool(container);
             Jedis jedis = pool.getResource()) {
            jedis.flushAll();
            log.debug("Redis data flushed");
        }
    }

    /**
     * Clean up all containers
     */
    public static void cleanup() {
        containers.values().forEach(container -> {
            if (container.isRunning()) {
                container.stop();
            }
        });
        containers.clear();
    }
}