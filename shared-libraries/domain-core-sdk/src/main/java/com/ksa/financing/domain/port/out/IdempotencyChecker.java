package com.ksa.financing.domain.port.out;

/**
 * Output port (SPI) for idempotency checking.
 * <p>
 * This interface defines the contract for idempotency checker implementations.
 * It enables use cases to detect and prevent duplicate processing of commands
 * or events with the same idempotency key.
 * </p>
 * <p>
 * Idempotency is critical for:
 * - Payment processing (prevent duplicate charges)
 * - Loan disbursements (prevent double disbursement)
 * - Event processing (prevent duplicate side effects)
 * - API requests (handle retries safely)
 * </p>
 * <p>
 * Implementation guidelines:
 * - Must use distributed storage (Redis, DynamoDB, etc.) for clustered deployments
 * - Should implement TTL-based expiration of old keys
 * - Must be atomic (check-and-set operation)
 * - Should handle race conditions properly
 * - Recommended TTL: 24-48 hours for payment operations
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface IdempotencyChecker {

    /**
     * Check if a request with the given key has already been processed.
     * <p>
     * This method checks whether a command or event with the specified
     * idempotency key has been previously processed successfully.
     * </p>
     * <p>
     * Idempotency key format recommendations:
     * - For API requests: "api:{endpoint}:{request-id}"
     * - For payments: "payment:{payment-reference}"
     * - For disbursements: "disbursement:{loan-id}:{date}"
     * - For events: "event:{event-id}"
     * </p>
     *
     * @param key the unique idempotency key
     * @return true if the request has been processed, false otherwise
     */
    boolean isProcessed(String key);

    /**
     * Mark a request as processed.
     * <p>
     * This method atomically marks the given idempotency key as processed,
     * typically after successful completion of the associated operation.
     * Implementations should use check-and-set semantics to prevent races.
     * </p>
     * <p>
     * The key should be stored with a TTL to prevent unbounded growth.
     * Recommended TTL: 24-48 hours for most operations.
     * </p>
     *
     * @param key the unique idempotency key
     * @return true if the key was successfully marked (first time), false if already marked
     */
    boolean markAsProcessed(String key);
}
