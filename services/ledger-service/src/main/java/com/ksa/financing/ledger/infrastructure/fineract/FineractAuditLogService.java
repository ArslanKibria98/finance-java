package com.ksa.financing.ledger.infrastructure.fineract;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.ledger.infrastructure.persistence.entity.FineractAuditLogJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.repository.JpaFineractAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Persists audit rows for every Fineract proxy call.
 * Fire-and-forget so it never blocks the proxy response path.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FineractAuditLogService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JpaFineractAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Async
    public void record(AuditEntry entry) {
        try {
            FineractAuditLogJpaEntity row = FineractAuditLogJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(entry.tenantId())
                    .callerService(entry.callerService())
                    .callerUserId(entry.callerUserId())
                    .operation(entry.operation())
                    .fineractEndpoint(entry.fineractEndpoint())
                    .httpMethod(entry.httpMethod())
                    .requestBody(entry.requestBody())
                    .responseBody(toMap(entry.responseBody()))
                    .responseStatus(entry.responseStatus())
                    .fineractResourceId(extractResourceId(entry.responseBody()))
                    .idempotencyKey(entry.idempotencyKey())
                    .correlationId(entry.correlationId())
                    .errorMessage(entry.errorMessage())
                    .durationMs(entry.durationMs())
                    .build();
            repository.save(row);
        } catch (Exception e) {
            // Audit must never break the proxy call
            log.error("Failed to persist Fineract audit log: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return objectMapper.convertValue(node, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("raw", node.toString());
        }
    }

    private String extractResourceId(JsonNode node) {
        if (node == null) return null;
        if (node.has("resourceId")) return node.get("resourceId").asText();
        if (node.has("savingsId")) return node.get("savingsId").asText();
        if (node.has("clientId")) return node.get("clientId").asText();
        if (node.has("loanId")) return node.get("loanId").asText();
        return null;
    }

    public record AuditEntry(
            UUID tenantId,
            String callerService,
            String callerUserId,
            String operation,
            String fineractEndpoint,
            String httpMethod,
            Map<String, Object> requestBody,
            JsonNode responseBody,
            Integer responseStatus,
            String idempotencyKey,
            String correlationId,
            String errorMessage,
            Integer durationMs
    ) {}
}
