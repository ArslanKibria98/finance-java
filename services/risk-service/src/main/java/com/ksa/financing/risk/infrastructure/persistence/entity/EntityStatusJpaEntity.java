package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "entity_status_records")
@Getter
@Setter
public class EntityStatusJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "entity_reference", nullable = false, length = 200)
    private String entityReference;

    @Column(name = "risk_status", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskStatusEnum riskStatus;

    @Column(name = "account_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatusEnum accountStatus;

    @Column(name = "compliance_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ComplianceStatusEnum complianceStatus;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum RiskStatusEnum { LOW, MEDIUM, HIGH, PEP }
    public enum AccountStatusEnum { ACTIVE, INACTIVE, PENDING, BLOCKED }
    public enum ComplianceStatusEnum { AUTO_APPROVED, COMPLIANCE_APPROVED, PENDING, REJECTED, BLOCKED }
}
