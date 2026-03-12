package com.ksa.financing.product.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "countries")
@Getter
@Setter
public class CountryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_ar", length = 100)
    private String nameAr;

    @Column(name = "dial_code", length = 10)
    private String dialCode;

    @Column(name = "alpha3_code", length = 3)
    private String alpha3Code;

    @Column(name = "numeric_code", length = 3)
    private String numericCode;

    @Column(name = "slug", length = 100)
    private String slug;

    @Column(name = "nationality_en", length = 100)
    private String nationalityEn;

    @Column(name = "nationality_ar", length = 100)
    private String nationalityAr;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "currency_name_en", length = 100)
    private String currencyNameEn;

    @Column(name = "currency_name_ar", length = 100)
    private String currencyNameAr;

    @Column(name = "flag_emoji", length = 10)
    private String flagEmoji;

    @Column(name = "capital_en", length = 100)
    private String capitalEn;

    @Column(name = "capital_ar", length = 100)
    private String capitalAr;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "sub_region", length = 50)
    private String subRegion;

    @Column(name = "is_gcc", nullable = false)
    private boolean isGcc;

    @Column(name = "is_arab_league", nullable = false)
    private boolean isArabLeague;

    @Column(name = "is_oic_member", nullable = false)
    private boolean isOicMember;

    @Column(name = "is_sanctioned", nullable = false)
    private boolean isSanctioned;

    @Column(name = "risk_tier", nullable = false, length = 20)
    private String riskTier;

    @Column(name = "iban_required", nullable = false)
    private boolean ibanRequired;

    @Column(name = "iban_length")
    private Integer ibanLength;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
