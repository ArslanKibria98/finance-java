package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eligibility_field_definitions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class EligibilityFieldDefinitionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "name_ar")
    private String nameAr;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description_ar", length = 500)
    private String descriptionAr;

    @Column(name = "data_type", nullable = false, length = 30)
    private String dataType;

    @Column(name = "input_type", nullable = false, length = 30)
    private String inputType;

    @Column(name = "placeholder_en")
    private String placeholderEn;

    @Column(name = "placeholder_ar")
    private String placeholderAr;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "min_value", precision = 18, scale = 2)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 2)
    private BigDecimal maxValue;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
