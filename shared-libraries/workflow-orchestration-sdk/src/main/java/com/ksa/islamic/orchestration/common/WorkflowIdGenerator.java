package com.ksa.islamic.orchestration.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generator for unique workflow IDs
 *
 * Provides various strategies for generating workflow IDs that are:
 * - Unique across the system
 * - Human-readable when possible
 * - Searchable and sortable
 */
public class WorkflowIdGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Generate a simple UUID-based workflow ID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a workflow ID with a prefix
     *
     * @param prefix The prefix for the ID (e.g., "LOAN", "DISB")
     */
    public static String generateWithPrefix(String prefix) {
        return String.format("%s-%s", prefix, UUID.randomUUID().toString());
    }

    /**
     * Generate a workflow ID with prefix and timestamp
     * Useful for chronological sorting
     *
     * @param prefix The prefix for the ID
     */
    public static String generateWithTimestamp(String prefix) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%s-%s", prefix, timestamp, uniqueId);
    }

    /**
     * Generate a workflow ID for a specific entity
     *
     * @param entityType The type of entity (e.g., "loan", "customer")
     * @param entityId The entity's unique identifier
     */
    public static String generateForEntity(String entityType, String entityId) {
        return String.format("%s-%s-%s",
                entityType.toUpperCase(),
                entityId,
                UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Generate a workflow ID for a specific operation on an entity
     *
     * @param entityType The type of entity
     * @param entityId The entity's unique identifier
     * @param operation The operation being performed
     */
    public static String generateForOperation(String entityType, String entityId, String operation) {
        return String.format("%s-%s-%s-%s",
                entityType.toUpperCase(),
                entityId,
                operation.toUpperCase(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * Generate a workflow ID for batch operations
     *
     * @param batchType The type of batch (e.g., "disbursement", "collection")
     * @param batchNumber A sequential batch number or identifier
     */
    public static String generateForBatch(String batchType, String batchNumber) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return String.format("BATCH-%s-%s-%s",
                batchType.toUpperCase(),
                timestamp,
                batchNumber);
    }

    /**
     * Generate a workflow ID for scheduled/cron workflows
     *
     * @param jobName The name of the scheduled job
     */
    public static String generateForScheduled(String jobName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return String.format("SCHED-%s-%s",
                jobName.toUpperCase(),
                timestamp);
    }

    /**
     * Generate a workflow ID with tenant context
     *
     * @param tenantId The tenant identifier
     * @param workflowType The type of workflow
     */
    public static String generateForTenant(String tenantId, String workflowType) {
        return String.format("T-%s-%s-%s",
                tenantId.toUpperCase(),
                workflowType.toUpperCase(),
                UUID.randomUUID().toString().substring(0, 12));
    }

    /**
     * Generate an idempotent workflow ID
     * Useful for ensuring exactly-once execution
     *
     * @param namespace A namespace for the idempotency key
     * @param key The idempotency key
     */
    public static String generateIdempotent(String namespace, String key) {
        return String.format("IDMP-%s-%s",
                namespace.toUpperCase(),
                key);
    }

    /**
     * Validate if a string is a valid workflow ID
     *
     * @param workflowId The workflow ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidWorkflowId(String workflowId) {
        if (workflowId == null || workflowId.isBlank()) {
            return false;
        }
        // Workflow IDs should not contain spaces or special characters except hyphen and underscore
        return workflowId.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Extract the prefix from a workflow ID
     *
     * @param workflowId The workflow ID
     * @return The prefix, or null if none found
     */
    public static String extractPrefix(String workflowId) {
        if (workflowId == null || !workflowId.contains("-")) {
            return null;
        }
        return workflowId.substring(0, workflowId.indexOf("-"));
    }
}