package com.ksa.financing.lending.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reschedule_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "reschedule_type", nullable = false)
    private String rescheduleType;

    @Column(name = "label_en", nullable = false)
    private String labelEn;

    @Column(name = "label_ar", nullable = false)
    private String labelAr;

    @Column(name = "description_en", length = 1000)
    private String descriptionEn;

    @Column(name = "description_ar", length = 1000)
    private String descriptionAr;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields_config", columnDefinition = "jsonb")
    private String fieldsConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_config", columnDefinition = "jsonb")
    private String rulesConfig;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "requires_approval")
    private boolean requiresApproval;

    @Column(name = "approver_role")
    private String approverRole;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
