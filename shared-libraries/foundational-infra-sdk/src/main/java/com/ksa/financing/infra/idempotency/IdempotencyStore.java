package com.ksa.financing.infra.idempotency;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface IdempotencyStore {

    <T> T executeIdempotent(String key, Supplier<T> operation);

    <T> T executeIdempotent(String key, Supplier<T> operation, Duration ttl);

    Optional<Object> get(String key);

    void store(String key, Object value);

    void store(String key, Object value, Duration ttl);

    void delete(String key);
}
