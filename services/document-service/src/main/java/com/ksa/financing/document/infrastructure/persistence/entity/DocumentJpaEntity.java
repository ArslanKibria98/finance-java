package com.ksa.financing.document.infrastructure.persistence.entity;

import com.ksa.financing.document.domain.model.DocumentKind;
import com.ksa.financing.document.domain.model.DocumentSourceFlow;
import com.ksa.financing.document.domain.model.DocumentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document_metadata")
public class DocumentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "workflow_id", length = 128)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "kind", nullable = false, columnDefinition = "document_kind")
    private DocumentKind kind;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "source_flow", nullable = false, columnDefinition = "document_source_flow")
    private DocumentSourceFlow sourceFlow;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "facia_reference_id", length = 100)
    private String faciaReferenceId;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "size_bytes_plain", nullable = false)
    private long sizeBytesPlain;

    @Column(name = "size_bytes_stored", nullable = false)
    private long sizeBytesStored;

    @Column(name = "sha256_plain", nullable = false, length = 64)
    private String sha256Plain;

    @Column(name = "cipher_algo", nullable = false, length = 32)
    private String cipherAlgo;

    @Column(name = "encrypted_key", nullable = false)
    private byte[] encryptedKey;

    @Column(name = "iv", nullable = false)
    private byte[] iv;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false, columnDefinition = "document_status")
    private DocumentStatus status;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
