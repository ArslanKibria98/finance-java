package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.DelinquencyType;

import java.util.List;
import java.util.UUID;

/**
 * Simple append-only port for delinquency-rule config changes.
 * Recorded from {@code ManageDelinquencyRuleUseCaseImpl} on every upsert/delete.
 */
public interface DelinquencyRuleAuditRepository {

    void record(UUID tenantId, UUID ruleId, UUID productId, DelinquencyType type,
                String action, String beforeJson, String afterJson,
                UUID actorId, String actorRole, String correlationId);

    List<AuditEntry> findByRule(UUID tenantId, UUID ruleId);

    List<AuditEntry> findByProduct(UUID tenantId, UUID productId);

    record AuditEntry(
            UUID id,
            UUID tenantId,
            UUID ruleId,
            UUID productId,
            DelinquencyType delinquencyType,
            String action,
            String beforeJson,
            String afterJson,
            UUID actorId,
            String actorRole,
            String correlationId,
            java.time.LocalDateTime changedAt
    ) {}
}
