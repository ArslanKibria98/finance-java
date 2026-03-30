package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "assessment_answers")
@Getter
@Setter
public class AssessmentAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "parameter_id", nullable = false)
    private UUID parameterId;

    @Column(name = "answer_version", nullable = false)
    private int answerVersion;

    @Column(name = "version_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private VersionStatusEnum versionStatus;

    @Column(name = "answer_value", length = 500)
    private String answerValue;

    @Column(name = "answer_type", length = 20)
    private String answerType;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "weight_contribution", precision = 10, scale = 4)
    private BigDecimal weightContribution;

    @Column(name = "risk_score_at_submission", precision = 10, scale = 2)
    private BigDecimal riskScoreAtSubmission;

    @Column(name = "change_reason", length = 30)
    @Enumerated(EnumType.STRING)
    private ChangeReasonEnum changeReason;

    @Column(name = "score_delta", precision = 10, scale = 4)
    private BigDecimal scoreDelta;

    @Column(name = "level_changed", nullable = false)
    private boolean levelChanged;

    @Column(name = "previous_risk_level", length = 20)
    private String previousRiskLevel;

    @Column(name = "new_risk_level", length = 20)
    private String newRiskLevel;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum VersionStatusEnum { ACTIVE, SUPERSEDED }
    public enum ChangeReasonEnum { CORRECTION, NEW_INFORMATION, RE_ASSESSMENT, REGULATORY_REQUEST }
}
