package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "assessment_sessions")
@Getter
@Setter
public class AssessmentSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "risk_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskParameterJpaEntity.RiskTypeEnum riskType;

    @Column(name = "entity_reference", nullable = false, length = 200)
    private String entityReference;

    @Column(name = "parent_session_id")
    private UUID parentSessionId;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private SessionStatusEnum status;

    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "pep_flag", nullable = false)
    private boolean pepFlag;

    @Column(name = "edd_flag", nullable = false)
    private boolean eddFlag;

    @Column(name = "kyc_flag", nullable = false)
    private boolean kycFlag;

    @Column(name = "dominant_override", nullable = false)
    private boolean dominantOverride;

    @Column(name = "third_party_aml_result", length = 50)
    private String thirdPartyAmlResult;

    @Column(name = "third_party_sanctions_result", length = 50)
    private String thirdPartySanctionsResult;

    @Column(name = "third_party_blocklist_result", length = 50)
    private String thirdPartyBlocklistResult;

    @Column(name = "parameter_version_snapshot")
    private Integer parameterVersionSnapshot;

    @Column(name = "lov_version_snapshot")
    private Integer lovVersionSnapshot;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum SessionStatusEnum {
        INITIATED, IN_PROGRESS, DRAFT_SAVED, SUBMITTED, SCORED,
        EDD_TRIGGERED, AML_CHECKED, COMPLETED, RE_EVALUATED
    }
}
