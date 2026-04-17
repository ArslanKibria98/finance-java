package com.ksa.financing.customer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "country_onboarding_profiles")
@Getter
@Setter
public class CountryOnboardingProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "step_type", nullable = false, length = 50)
    private String stepType;

    @Column(name = "step_label", nullable = false, length = 100)
    private String stepLabel;

    @Column(name = "step_label_ar", length = 100)
    private String stepLabelAr;

    @Column(name = "description")
    private String description;

    @Column(name = "provider_code", nullable = false, length = 50)
    private String providerCode;

    @Column(name = "is_signal_wait", nullable = false)
    private boolean signalWait;

    @Column(name = "timeout_minutes", nullable = false)
    private int timeoutMinutes;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
