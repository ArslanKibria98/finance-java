package com.ksa.financing.product.infrastructure.persistence.mapper;

import com.ksa.financing.product.domain.model.TemplateType;
import com.ksa.financing.product.infrastructure.persistence.entity.TemplateTypeJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class TemplateTypePersistenceMapper {

    public static TemplateType toDomain(TemplateTypeJpaEntity entity) {
        if (entity == null) return null;

        var domain = new TemplateType();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setCategory(entity.getCategory());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        return domain;
    }

    public static TemplateTypeJpaEntity toEntity(TemplateType domain) {
        if (domain == null) return null;

        var entity = new TemplateTypeJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setCategory(domain.getCategory());
        entity.setActive(domain.isActive());
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
