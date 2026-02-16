package com.ksa.islamic.orchestration.common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Workflow context for metadata and tenant information
 *
 * Carries contextual information through workflow execution,
 * including tenant context, user information, and custom metadata.
 */
@Data
@Builder
public class WorkflowContext {

    /**
     * The tenant ID for multi-tenant scenarios
     */
    private String tenantId;

    /**
     * The user who initiated the workflow
     */
    private String initiatedBy;

    /**
     * The timestamp when the workflow was initiated
     */
    @Builder.Default
    private LocalDateTime initiatedAt = LocalDateTime.now();

    /**
     * The source system or channel that triggered the workflow
     */
    private String source;

    /**
     * Correlation ID for tracing across systems
     */
    private String correlationId;

    /**
     * Request ID from the original API call
     */
    private String requestId;

    /**
     * Custom metadata for the workflow
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Business context (e.g., product type, channel)
     */
    @Builder.Default
    private Map<String, String> businessContext = new HashMap<>();

    /**
     * Security context for authorization
     */
    private SecurityContext securityContext;

    /**
     * Add metadata to the context
     */
    public WorkflowContext withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Add business context
     */
    public WorkflowContext withBusinessContext(String key, String value) {
        this.businessContext.put(key, value);
        return this;
    }

    /**
     * Get metadata value with type safety
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Check if context is for a specific tenant
     */
    public boolean isForTenant(String tenantId) {
        return this.tenantId != null && this.tenantId.equals(tenantId);
    }

    /**
     * Create a default context
     */
    public static WorkflowContext createDefault() {
        return WorkflowContext.builder()
                .tenantId("default")
                .source("system")
                .build();
    }

    /**
     * Create context for a specific tenant
     */
    public static WorkflowContext forTenant(String tenantId) {
        return WorkflowContext.builder()
                .tenantId(tenantId)
                .build();
    }

    /**
     * Create context from an API request
     */
    public static WorkflowContext fromRequest(String tenantId, String userId, String requestId) {
        return WorkflowContext.builder()
                .tenantId(tenantId)
                .initiatedBy(userId)
                .requestId(requestId)
                .source("api")
                .build();
    }

    /**
     * Security context for authorization within workflows
     */
    @Data
    @Builder
    public static class SecurityContext {
        private String userId;
        private String[] roles;
        private Map<String, String> permissions;
        private String authToken;

        /**
         * Check if user has a specific role
         */
        public boolean hasRole(String role) {
            if (roles == null) return false;
            for (String r : roles) {
                if (r.equals(role)) return true;
            }
            return false;
        }

        /**
         * Check if user has a specific permission
         */
        public boolean hasPermission(String permission) {
            return permissions != null && permissions.containsKey(permission);
        }
    }
}