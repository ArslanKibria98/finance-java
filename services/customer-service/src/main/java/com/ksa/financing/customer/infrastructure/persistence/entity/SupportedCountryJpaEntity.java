package com.ksa.financing.customer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supported_countries")
@Getter
@Setter
public class SupportedCountryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false, unique = true, length = 3)
    private String countryCode;

    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Column(name = "country_name_ar", length = 100)
    private String countryNameAr;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "id_types", nullable = false)
    private String idTypes;

    @Column(name = "default_id_type", nullable = false, length = 20)
    private String defaultIdType;

    @Column(name = "kyc_providers", nullable = false)
    private String kycProviders;

    @Column(name = "flag_emoji", length = 10)
    private String flagEmoji;

    @Column(name = "dial_code", length = 10)
    private String dialCode;

    @Column(name = "nationality_en", length = 50)
    private String nationalityEn;

    @Column(name = "nationality_ar", length = 50)
    private String nationalityAr;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
