package com.ksa.financing.risk.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_parameters")
@Getter
@Setter
public class RiskParameterJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "risk_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskTypeEnum riskType;

    @Column(name = "flow", length = 100)
    private String flow;

    @Column(name = "category", nullable = false, length = 200)
    private String category;

    @Column(name = "sub_category", length = 200)
    private String subCategory;

    @Column(name = "question_en", nullable = false, length = 500)
    private String questionEn;

    @Column(name = "question_ar", length = 500)
    private String questionAr;

    @Column(name = "input_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InputTypeEnum inputType;

    @Column(name = "lov_set_id")
    private UUID lovSetId;

    @Column(name = "parent_parameter_id")
    private UUID parentParameterId;

    @Column(name = "parent_trigger_value", length = 200)
    private String parentTriggerValue;

    @Column(name = "category_weight", nullable = false, precision = 10, scale = 4)
    private BigDecimal categoryWeight;

    @Column(name = "operator", length = 20)
    private String operator;

    @Column(name = "expected_value", length = 200)
    private String expectedValue;

    @Column(name = "flag_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private FlagTypeEnum flagType;

    @Column(name = "filled_by", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FilledByEnum filledBy;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    public enum RiskTypeEnum { CUSTOMER, BUSINESS, LOAN }
    public enum InputTypeEnum { TEXT, BOOLEAN, LOV, LINKED_LOV }
    public enum FlagTypeEnum { PEP, EDD, KYC, NONE }
    public enum FilledByEnum { CUSTOMER, SYSTEM_USER, AUTO_API }
}
