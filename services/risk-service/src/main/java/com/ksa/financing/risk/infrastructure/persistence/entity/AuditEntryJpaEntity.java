package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_entries")
@Getter
@Setter
public class AuditEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "entity_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AuditEntityTypeEnum entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum AuditEntityTypeEnum {
        ASSESSMENT_SESSION, ASSESSMENT_ANSWER, ENTITY_STATUS, REVIEW_TASK,
        SCENARIO_RULE, LOV_SET, LOV_ENTRY, RISK_PARAMETER, SCORING_THRESHOLD,
        TENANT_CONFIG, BLACKLIST, FRAUD_RULE
    }
}
