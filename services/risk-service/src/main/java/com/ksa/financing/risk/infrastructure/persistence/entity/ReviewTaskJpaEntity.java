package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_tasks")
@Getter
@Setter
public class ReviewTaskJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "entity_reference", nullable = false, length = 200)
    private String entityReference;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ReviewStatusEnum status;

    @Column(name = "maker_id")
    private UUID makerId;

    @Column(name = "maker_recommendation", length = 20)
    @Enumerated(EnumType.STRING)
    private RecommendationEnum makerRecommendation;

    @Column(name = "maker_comment", length = 1000)
    private String makerComment;

    @Column(name = "maker_account_status", length = 20)
    @Enumerated(EnumType.STRING)
    private EntityStatusJpaEntity.AccountStatusEnum makerAccountStatus;

    @Column(name = "maker_compliance_status", length = 30)
    @Enumerated(EnumType.STRING)
    private EntityStatusJpaEntity.ComplianceStatusEnum makerComplianceStatus;

    @Column(name = "maker_action_at")
    private OffsetDateTime makerActionAt;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "approver_action", length = 20)
    @Enumerated(EnumType.STRING)
    private ApproverActionEnum approverAction;

    @Column(name = "approver_comment", length = 1000)
    private String approverComment;

    @Column(name = "approver_account_status", length = 20)
    @Enumerated(EnumType.STRING)
    private EntityStatusJpaEntity.AccountStatusEnum approverAccountStatus;

    @Column(name = "approver_compliance_status", length = 30)
    @Enumerated(EnumType.STRING)
    private EntityStatusJpaEntity.ComplianceStatusEnum approverComplianceStatus;

    @Column(name = "approver_action_at")
    private OffsetDateTime approverActionAt;

    @Column(name = "sla_deadline")
    private OffsetDateTime slaDeadline;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum ReviewStatusEnum {
        PENDING_MAKER, MAKER_RECOMMENDED, PENDING_APPROVER, APPROVED, REJECTED, ESCALATED, EXPIRED
    }
    public enum RecommendationEnum { APPROVE, REJECT, ESCALATE }
    public enum ApproverActionEnum { RECOMMEND_APPROVE, RECOMMEND_REJECT, APPROVE, REJECT, ESCALATE, RETURN_TO_MAKER }
}
