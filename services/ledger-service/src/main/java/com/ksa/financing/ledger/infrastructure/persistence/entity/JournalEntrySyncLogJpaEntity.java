package com.ksa.financing.ledger.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for `journal_entry_sync_log` table.
 * Tracks per-entry sync state with Fineract.
 */
@Entity
@Table(
        name = "journal_entry_sync_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_journal_sync", columnNames = {"tenant_id", "journal_entry_id"})
        },
        indexes = {
                @Index(name = "idx_sync_log_status", columnList = "sync_status"),
                @Index(name = "idx_sync_log_retry", columnList = "next_retry_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntrySyncLogJpaEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "journal_entry_id", nullable = false)
    private UUID journalEntryId;

    @Column(name = "fineract_transaction_id")
    private Long fineractTransactionId;

    @Column(name = "fineract_office_id")
    private Long fineractOfficeId;

    @Column(name = "sync_status", nullable = false, length = 30)
    private String syncStatus;

    @Column(name = "sync_attempts", nullable = false)
    private int syncAttempts;

    @Column(name = "max_sync_attempts", nullable = false)
    private int maxSyncAttempts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private Map<String, Object> responsePayload;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "first_attempt_at")
    private LocalDateTime firstAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
