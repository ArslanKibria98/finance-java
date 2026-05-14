package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_rules")
@Getter
@Setter
public class FraudRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_id", nullable = false, length = 20)
    private String ruleId;

    @Column(name = "scenario_name", nullable = false, length = 200)
    private String scenarioName;

    @Column(name = "scenario_name_ar", length = 200)
    private String scenarioNameAr;

    @Column(name = "category", nullable = false, columnDefinition = "fraud_rule_category")
    @Enumerated(EnumType.STRING)
    private FraudRuleCategory category;

    @Column(name = "detection_logic", columnDefinition = "TEXT")
    private String detectionLogic;

    @Column(name = "default_action", nullable = false, columnDefinition = "fraud_decision_type")
    @Enumerated(EnumType.STRING)
    private FraudDecisionType defaultAction;

    @Column(name = "block_type", columnDefinition = "fraud_block_type")
    @Enumerated(EnumType.STRING)
    private FraudBlockType blockType;

    @Column(name = "status", nullable = false, columnDefinition = "fraud_rule_status")
    @Enumerated(EnumType.STRING)
    private FraudRuleStatus status;

    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "block_code_id")
    private UUID blockCodeId;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum FraudRuleCategory {
        LOCATION, DEVICE, GEOGRAPHIC_ACCESS, FINANCIAL, PAYMENT_CARD, TRANSACTION_MONITORING
    }

    public enum FraudDecisionType {
        ALLOW, ALERT, HOLD, BLOCK
    }

    public enum FraudBlockType {
        TEMPORARY, PERMANENT, SESSION, APPLICATION_LEVEL, DISBURSEMENT_LEVEL, PAYMENT_LEVEL
    }

    public enum FraudRuleStatus {
        ACTIVE, DISABLED, TESTING
    }
}
