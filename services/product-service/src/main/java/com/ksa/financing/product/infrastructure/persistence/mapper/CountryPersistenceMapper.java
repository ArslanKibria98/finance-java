package com.ksa.financing.product.infrastructure.persistence.mapper;

import com.ksa.financing.product.domain.model.Country;
import com.ksa.financing.product.infrastructure.persistence.entity.CountryJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CountryPersistenceMapper {

    public static Country toDomain(CountryJpaEntity entity) {
        if (entity == null) return null;

        var domain = new Country();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCode(entity.getCode());
        domain.setAlpha3Code(entity.getAlpha3Code());
        domain.setNumericCode(entity.getNumericCode());
        domain.setSlug(entity.getSlug());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setNationalityEn(entity.getNationalityEn());
        domain.setNationalityAr(entity.getNationalityAr());
        domain.setDialCode(entity.getDialCode());
        domain.setCurrencyCode(entity.getCurrencyCode());
        domain.setCurrencyNameEn(entity.getCurrencyNameEn());
        domain.setCurrencyNameAr(entity.getCurrencyNameAr());
        domain.setFlagEmoji(entity.getFlagEmoji());
        domain.setCapitalEn(entity.getCapitalEn());
        domain.setCapitalAr(entity.getCapitalAr());
        domain.setRegion(entity.getRegion());
        domain.setSubRegion(entity.getSubRegion());
        domain.setGcc(entity.isGcc());
        domain.setArabLeague(entity.isArabLeague());
        domain.setOicMember(entity.isOicMember());
        domain.setSanctioned(entity.isSanctioned());
        domain.setRiskTier(entity.getRiskTier());
        domain.setIbanRequired(entity.isIbanRequired());
        domain.setIbanLength(entity.getIbanLength());
        domain.setActive(entity.isActive());
        domain.setSortOrder(entity.getSortOrder());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        return domain;
    }

    public static CountryJpaEntity toEntity(Country domain) {
        if (domain == null) return null;

        var entity = new CountryJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCode(domain.getCode());
        entity.setAlpha3Code(domain.getAlpha3Code());
        entity.setNumericCode(domain.getNumericCode());
        entity.setSlug(domain.getSlug());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setNationalityEn(domain.getNationalityEn());
        entity.setNationalityAr(domain.getNationalityAr());
        entity.setDialCode(domain.getDialCode());
        entity.setCurrencyCode(domain.getCurrencyCode());
        entity.setCurrencyNameEn(domain.getCurrencyNameEn());
        entity.setCurrencyNameAr(domain.getCurrencyNameAr());
        entity.setFlagEmoji(domain.getFlagEmoji());
        entity.setCapitalEn(domain.getCapitalEn());
        entity.setCapitalAr(domain.getCapitalAr());
        entity.setRegion(domain.getRegion());
        entity.setSubRegion(domain.getSubRegion());
        entity.setGcc(domain.isGcc());
        entity.setArabLeague(domain.isArabLeague());
        entity.setOicMember(domain.isOicMember());
        entity.setSanctioned(domain.isSanctioned());
        entity.setRiskTier(domain.getRiskTier());
        entity.setIbanRequired(domain.isIbanRequired());
        entity.setIbanLength(domain.getIbanLength());
        entity.setActive(domain.isActive());
        entity.setSortOrder(domain.getSortOrder());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        return entity;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}
