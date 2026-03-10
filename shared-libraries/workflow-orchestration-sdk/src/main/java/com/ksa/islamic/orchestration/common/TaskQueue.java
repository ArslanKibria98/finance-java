package com.ksa.islamic.orchestration.common;

/**
 * Standard task queue names for all services
 *
 * Centralizes task queue naming to ensure consistency across
 * the platform and prevent misconfigurations.
 */
public final class TaskQueue {

    // Core service task queues
    public static final String LENDING_QUEUE = "lending-task-queue";
    public static final String SHARIA_QUEUE = "sharia-task-queue";
    public static final String LEDGER_QUEUE = "ledger-task-queue";
    public static final String CUSTOMER_QUEUE = "customer-task-queue";
    public static final String COLLECTION_QUEUE = "collection-task-queue";
    public static final String NOTIFICATION_QUEUE = "notification-task-queue";
    public static final String IDENTITY_QUEUE = "identity-task-queue";
    public static final String RISK_ASSESSMENT_QUEUE = "risk-assessment-queue";
    public static final String KYC_QUEUE = "kyc-task-queue";
    public static final String ONBOARDING_QUEUE = "onboarding-task-queue";

    // Specialized task queues
    public static final String WALLET_QUEUE = "wallet-task-queue";
    public static final String PAYMENT_QUEUE = "payment-task-queue";
    public static final String DOCUMENT_QUEUE = "document-task-queue";
    public static final String REPORTING_QUEUE = "reporting-task-queue";
    public static final String AUDIT_QUEUE = "audit-task-queue";

    // Batch processing queues
    public static final String BATCH_DISBURSEMENT_QUEUE = "batch-disbursement-queue";
    public static final String BATCH_COLLECTION_QUEUE = "batch-collection-queue";
    public static final String BATCH_ACCRUAL_QUEUE = "batch-accrual-queue";
    public static final String BATCH_REPORTING_QUEUE = "batch-reporting-queue";

    // External integration queues
    public static final String SIMAH_QUEUE = "simah-integration-queue";
    public static final String NAFATH_QUEUE = "nafath-integration-queue";
    public static final String SADAD_QUEUE = "sadad-integration-queue";
    public static final String GOVERNMENT_QUEUE = "government-api-queue";

    // Default queue for general purposes
    public static final String DEFAULT_QUEUE = "default-task-queue";

    private TaskQueue() {
        // Prevent instantiation
    }

    /**
     * Get task queue name for a specific service
     *
     * @param serviceName The name of the service
     * @return The task queue name for the service
     */
    public static String forService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return DEFAULT_QUEUE;
        }
        return serviceName.toLowerCase() + "-task-queue";
    }

    /**
     * Get task queue name for a specific tenant
     * Useful for multi-tenant scenarios where isolation is required
     *
     * @param baseQueue The base queue name
     * @param tenantId The tenant identifier
     * @return The tenant-specific task queue name
     */
    public static String forTenant(String baseQueue, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return baseQueue;
        }
        return baseQueue + "-" + tenantId.toLowerCase();
    }

    /**
     * Validate if a queue name follows naming conventions
     *
     * @param queueName The queue name to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidQueueName(String queueName) {
        if (queueName == null || queueName.isBlank()) {
            return false;
        }
        // Queue names should be lowercase with hyphens only
        return queueName.matches("^[a-z0-9-]+$");
    }
}