package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scenario_rules")
@Getter
@Setter
public class ScenarioRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "scenario_name", nullable = false, length = 200)
    private String scenarioName;

    @Column(name = "scenario_name_ar", length = 200)
    private String scenarioNameAr;

    @Column(name = "trigger_risk_status", length = 20)
    private String triggerRiskStatus;

    @Column(name = "trigger_pep_flag")
    private Boolean triggerPepFlag;

    @Column(name = "trigger_third_party_check_type", length = 20)
    @Enumerated(EnumType.STRING)
    private ThirdPartyCheckTypeEnum triggerThirdPartyCheckType;

    @Column(name = "trigger_third_party_result", length = 50)
    private String triggerThirdPartyResult;

    @Column(name = "resulting_account_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EntityStatusJpaEntity.AccountStatusEnum resultingAccountStatus;

    @Column(name = "resulting_compliance_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EntityStatusJpaEntity.ComplianceStatusEnum resultingComplianceStatus;

    @Column(name = "requires_manual_review", nullable = false)
    private boolean requiresManualReview;

    @Column(name = "notify_role", length = 100)
    private String notifyRole;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "sla_duration_hours", nullable = false)
    private int slaDurationHours;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum ThirdPartyCheckTypeEnum { AML, SANCTIONS, LOCAL_TEST, BLOCKLIST }
}
