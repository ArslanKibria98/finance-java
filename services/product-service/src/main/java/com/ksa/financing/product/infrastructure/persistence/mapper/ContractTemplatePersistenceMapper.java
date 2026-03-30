package com.ksa.financing.product.infrastructure.persistence.mapper;

import com.ksa.financing.product.domain.model.ContractTemplate;
import com.ksa.financing.product.infrastructure.persistence.entity.ContractTemplateJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class ContractTemplatePersistenceMapper {

    public static ContractTemplate toDomain(ContractTemplateJpaEntity entity) {
        if (entity == null) return null;

        var domain = new ContractTemplate();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setNameEn(entity.getNameEn());
        domain.setNameAr(entity.getNameAr());
        domain.setProductId(entity.getProductId());
        domain.setTypeId(entity.getTypeId());
        domain.setLanguage(entity.getLanguage());
        domain.setMessage(entity.getMessage());
        domain.setActive(entity.isActive());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        return domain;
    }

    public static ContractTemplateJpaEntity toEntity(ContractTemplate domain) {
        if (domain == null) return null;

        var entity = new ContractTemplateJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setNameEn(domain.getNameEn());
        entity.setNameAr(domain.getNameAr());
        entity.setProductId(domain.getProductId());
        entity.setTypeId(domain.getTypeId());
        entity.setLanguage(domain.getLanguage());
        entity.setMessage(domain.getMessage());
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
