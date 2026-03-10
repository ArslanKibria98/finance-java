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
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setDialCode(entity.getDialCode());
        domain.setCurrencyCode(entity.getCurrencyCode());
        domain.setGcc(entity.isGcc());
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
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setDialCode(domain.getDialCode());
        entity.setCurrencyCode(domain.getCurrencyCode());
        entity.setGcc(domain.isGcc());
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
