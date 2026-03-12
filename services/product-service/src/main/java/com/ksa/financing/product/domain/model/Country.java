package com.ksa.financing.product.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Country domain model — pure POJO.
 */
public class Country {

    private UUID id;
    private UUID tenantId;
    private String code;
    private String alpha3Code;
    private String numericCode;
    private String slug;
    private String nameEn;
    private String nameAr;
    private String nationalityEn;
    private String nationalityAr;
    private String dialCode;
    private String currencyCode;
    private String currencyNameEn;
    private String currencyNameAr;
    private String flagEmoji;
    private String capitalEn;
    private String capitalAr;
    private String region;
    private String subRegion;
    private boolean gcc;
    private boolean arabLeague;
    private boolean oicMember;
    private boolean sanctioned;
    private String riskTier;
    private boolean ibanRequired;
    private Integer ibanLength;
    private boolean active;
    private int sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    public Country() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getAlpha3Code() { return alpha3Code; }
    public void setAlpha3Code(String alpha3Code) { this.alpha3Code = alpha3Code; }

    public String getNumericCode() { return numericCode; }
    public void setNumericCode(String numericCode) { this.numericCode = numericCode; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getNationalityEn() { return nationalityEn; }
    public void setNationalityEn(String nationalityEn) { this.nationalityEn = nationalityEn; }

    public String getNationalityAr() { return nationalityAr; }
    public void setNationalityAr(String nationalityAr) { this.nationalityAr = nationalityAr; }

    public String getDialCode() { return dialCode; }
    public void setDialCode(String dialCode) { this.dialCode = dialCode; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getCurrencyNameEn() { return currencyNameEn; }
    public void setCurrencyNameEn(String currencyNameEn) { this.currencyNameEn = currencyNameEn; }

    public String getCurrencyNameAr() { return currencyNameAr; }
    public void setCurrencyNameAr(String currencyNameAr) { this.currencyNameAr = currencyNameAr; }

    public String getFlagEmoji() { return flagEmoji; }
    public void setFlagEmoji(String flagEmoji) { this.flagEmoji = flagEmoji; }

    public String getCapitalEn() { return capitalEn; }
    public void setCapitalEn(String capitalEn) { this.capitalEn = capitalEn; }

    public String getCapitalAr() { return capitalAr; }
    public void setCapitalAr(String capitalAr) { this.capitalAr = capitalAr; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getSubRegion() { return subRegion; }
    public void setSubRegion(String subRegion) { this.subRegion = subRegion; }

    public boolean isGcc() { return gcc; }
    public void setGcc(boolean gcc) { this.gcc = gcc; }

    public boolean isArabLeague() { return arabLeague; }
    public void setArabLeague(boolean arabLeague) { this.arabLeague = arabLeague; }

    public boolean isOicMember() { return oicMember; }
    public void setOicMember(boolean oicMember) { this.oicMember = oicMember; }

    public boolean isSanctioned() { return sanctioned; }
    public void setSanctioned(boolean sanctioned) { this.sanctioned = sanctioned; }

    public String getRiskTier() { return riskTier; }
    public void setRiskTier(String riskTier) { this.riskTier = riskTier; }

    public boolean isIbanRequired() { return ibanRequired; }
    public void setIbanRequired(boolean ibanRequired) { this.ibanRequired = ibanRequired; }

    public Integer getIbanLength() { return ibanLength; }
    public void setIbanLength(Integer ibanLength) { this.ibanLength = ibanLength; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
