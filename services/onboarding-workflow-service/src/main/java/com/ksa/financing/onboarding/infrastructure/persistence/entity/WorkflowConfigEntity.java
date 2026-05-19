package com.ksa.financing.onboarding.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
@Table(name = "workflow_configs")
@Data
public class WorkflowConfigEntity {
    @Id
    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
