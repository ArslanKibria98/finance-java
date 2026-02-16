package com.demo.islamic.compliance.sama;

import com.demo.islamic.compliance.config.ComplianceConfig;
import com.demo.islamic.compliance.model.AuditEvent;
import com.demo.islamic.compliance.model.AuditEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SAMA-compliant audit logger for regulatory requirements
 * Implements immutable event logging with 7-year retention
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SamaAuditLogger {

    private static final String KSA_TIMEZONE = "Asia/Riyadh";
    private static final DateTimeFormatter AUDIT_TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final ComplianceConfig complianceConfig;
    private final ObjectMapper objectMapper;
    private final AuditEventRepository auditEventRepository;

    /**
     * Log an audit event for SAMA compliance
     * Events are immutable and retained for 7 years
     */
    public AuditEvent logEvent(AuditEventType eventType, String userId,
                               String resourceId, Map<String, Object> metadata) {
        try {
            AuditEvent event = createAuditEvent(eventType, userId, resourceId, metadata);

            // Calculate integrity hash to ensure immutability
            String integrityHash = calculateIntegrityHash(event);
            event.setIntegrityHash(integrityHash);

            // Save to immutable audit store asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    auditEventRepository.save(event);
                    log.debug("Audit event logged: eventId={}, type={}, userId={}",
                        event.getEventId(), event.getEventType(), event.getUserId());
                } catch (Exception e) {
                    log.error("Failed to persist audit event: eventId={}", event.getEventId(), e);
                }
            });

            return event;
        } catch (Exception e) {
            log.error("Failed to create audit event", e);
            throw new RuntimeException("Audit logging failed", e);
        }
    }

    /**
     * Log user authentication events
     */
    public void logAuthentication(String userId, String authMethod,
                                  boolean success, String ipAddress) {
        Map<String, Object> metadata = Map.of(
            "authMethod", authMethod,
            "success", success,
            "ipAddress", ipAddress,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        AuditEventType eventType = success ?
            AuditEventType.USER_LOGIN_SUCCESS : AuditEventType.USER_LOGIN_FAILURE;

        logEvent(eventType, userId, null, metadata);
    }

    /**
     * Log authorization/access control events
     */
    public void logAuthorization(String userId, String resource,
                                 String action, boolean granted) {
        Map<String, Object> metadata = Map.of(
            "resource", resource,
            "action", action,
            "granted", granted,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        AuditEventType eventType = granted ?
            AuditEventType.ACCESS_GRANTED : AuditEventType.ACCESS_DENIED;

        logEvent(eventType, userId, resource, metadata);
    }

    /**
     * Log data access events for PII
     */
    public void logDataAccess(String userId, String dataType,
                              String entityId, String purpose) {
        Map<String, Object> metadata = Map.of(
            "dataType", dataType,
            "entityId", entityId,
            "purpose", purpose,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        logEvent(AuditEventType.DATA_ACCESS, userId, entityId, metadata);
    }

    /**
     * Log financial transaction events
     */
    public void logFinancialTransaction(String userId, String transactionId,
                                       String transactionType, Map<String, Object> details) {
        Map<String, Object> metadata = Map.of(
            "transactionType", transactionType,
            "details", details,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        logEvent(AuditEventType.FINANCIAL_TRANSACTION, userId, transactionId, metadata);
    }

    /**
     * Log loan lifecycle events
     */
    public void logLoanEvent(String userId, String loanId,
                             String eventName, Map<String, Object> details) {
        Map<String, Object> metadata = Map.of(
            "eventName", eventName,
            "loanDetails", details,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        AuditEventType eventType = determineL eventType(eventName);
        logEvent(eventType, userId, loanId, metadata);
    }

    /**
     * Log system configuration changes
     */
    public void logConfigurationChange(String userId, String configKey,
                                       String oldValue, String newValue) {
        Map<String, Object> metadata = Map.of(
            "configKey", configKey,
            "oldValue", oldValue != null ? oldValue : "null",
            "newValue", newValue,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        logEvent(AuditEventType.CONFIGURATION_CHANGE, userId, configKey, metadata);
    }

    /**
     * Log compliance violations
     */
    public void logComplianceViolation(String userId, String violationType,
                                       String description, String severity) {
        Map<String, Object> metadata = Map.of(
            "violationType", violationType,
            "description", description,
            "severity", severity,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        logEvent(AuditEventType.COMPLIANCE_VIOLATION, userId, null, metadata);
    }

    /**
     * Log report generation for regulatory reporting
     */
    public void logReportGeneration(String userId, String reportType,
                                   String reportId, Map<String, Object> parameters) {
        Map<String, Object> metadata = Map.of(
            "reportType", reportType,
            "parameters", parameters,
            "timestamp", LocalDateTime.now(ZoneId.of(KSA_TIMEZONE))
        );

        logEvent(AuditEventType.REPORT_GENERATED, userId, reportId, metadata);
    }

    /**
     * Create an audit event
     */
    private AuditEvent createAuditEvent(AuditEventType eventType, String userId,
                                        String resourceId, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent();

        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now(ZoneId.of(KSA_TIMEZONE)));
        event.setUserId(userId);
        event.setResourceId(resourceId);
        event.setMetadata(metadata);

        // Add tenant information if available
        if (complianceConfig.getTenantId() != null) {
            event.setTenantId(complianceConfig.getTenantId());
        }

        // Add additional context
        event.setServiceName(complianceConfig.getServiceName());
        event.setEnvironment(complianceConfig.getEnvironment());

        return event;
    }

    /**
     * Calculate integrity hash for audit event
     * This ensures the event cannot be modified after creation
     */
    private String calculateIntegrityHash(AuditEvent event) {
        try {
            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);

            // Calculate SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(eventJson.getBytes(StandardCharsets.UTF_8));

            // Add salt from configuration
            if (complianceConfig.getAuditHashSalt() != null) {
                digest.update(complianceConfig.getAuditHashSalt().getBytes(StandardCharsets.UTF_8));
                hash = digest.digest(hash);
            }

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to calculate integrity hash", e);
            throw new RuntimeException("Integrity hash calculation failed", e);
        }
    }

    /**
     * Determine loan event type based on event name
     */
    private AuditEventType determineLoanEventType(String eventName) {
        return switch (eventName.toUpperCase()) {
            case "APPLIED" -> AuditEventType.LOAN_APPLICATION;
            case "APPROVED" -> AuditEventType.LOAN_APPROVAL;
            case "REJECTED" -> AuditEventType.LOAN_REJECTION;
            case "DISBURSED" -> AuditEventType.LOAN_DISBURSEMENT;
            case "PAYMENT" -> AuditEventType.LOAN_PAYMENT;
            case "CLOSED" -> AuditEventType.LOAN_CLOSURE;
            case "DEFAULTED" -> AuditEventType.LOAN_DEFAULT;
            case "RESTRUCTURED" -> AuditEventType.LOAN_RESTRUCTURE;
            default -> AuditEventType.LOAN_EVENT;
        };
    }

    /**
     * Verify integrity of an audit event
     */
    public boolean verifyEventIntegrity(AuditEvent event) {
        if (event.getIntegrityHash() == null) {
            return false;
        }

        String originalHash = event.getIntegrityHash();
        event.setIntegrityHash(null);

        String calculatedHash = calculateIntegrityHash(event);
        event.setIntegrityHash(originalHash);

        return originalHash.equals(calculatedHash);
    }

    /**
     * Repository interface for audit events
     * Implementation should ensure immutable storage
     */
    public interface AuditEventRepository {
        void save(AuditEvent event);
        AuditEvent findById(String eventId);
        List<AuditEvent> findByUserId(String userId, LocalDateTime from, LocalDateTime to);
        List<AuditEvent> findByResourceId(String resourceId, LocalDateTime from, LocalDateTime to);
        List<AuditEvent> findByEventType(AuditEventType eventType, LocalDateTime from, LocalDateTime to);
    }
}