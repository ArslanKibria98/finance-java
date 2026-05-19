package com.ksa.financing.onboarding.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "field_configs")
@Data
public class FieldConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private StepConfigEntity step;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @Column(name = "field_type", nullable = false)
    private String fieldType;

    @Column(name = "is_pii")
    private Boolean isPii = false;

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    @Column(name = "validation_regex")
    private String validationRegex;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
