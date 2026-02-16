package com.ksa.islamic.orchestration.config;

import lombok.Builder;
import lombok.Data;

/**
 * Namespace configuration for multi-tenant support
 */
@Data
@Builder
public class NamespaceConfig {
    private String defaultNamespace;

    /**
     * Gets namespace for a specific tenant
     */
    public String getNamespaceForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return defaultNamespace;
        }
        // In production, each tenant could have its own namespace
        // For now, we use a single namespace with tenant context in workflow metadata
        return defaultNamespace;
    }
}