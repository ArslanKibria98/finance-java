package com.ksa.financing.ledger.infrastructure.persistence.mapper;

import com.ksa.financing.ledger.domain.model.CoaConfigurationMapping;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;
import com.ksa.financing.ledger.domain.model.CoaConfigurationStatus;
import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaConfigurationMappingJpaEntity;
import com.ksa.financing.ledger.infrastructure.persistence.entity.CoaConfigurationProfileJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CoaConfigurationPersistenceMapper {

    public CoaConfigurationProfileJpaEntity toJpaEntity(CoaConfigurationProfile domain) {
        return CoaConfigurationProfileJpaEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .productCode(domain.getProductCode())
                .profileName(domain.getProfileName())
                .status(domain.getStatus().name())
                .effectiveFrom(domain.getEffectiveFrom())
                .effectiveTo(domain.getEffectiveTo())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    public CoaConfigurationProfile toDomain(CoaConfigurationProfileJpaEntity entity) {
        return CoaConfigurationProfile.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .productCode(entity.getProductCode())
                .profileName(entity.getProfileName())
                .status(CoaConfigurationStatus.valueOf(entity.getStatus()))
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public CoaConfigurationMappingJpaEntity toJpaEntity(CoaConfigurationMapping domain) {
        return CoaConfigurationMappingJpaEntity.builder()
                .id(domain.id())
                .tenantId(domain.tenantId())
                .profileId(domain.profileId())
                .coaFieldId(domain.coaFieldId())
                .accountId(domain.accountId())
                .mandatoryOverride(domain.mandatoryOverride())
                .notes(domain.notes())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .version(domain.version())
                .build();
    }

    public CoaConfigurationMapping toDomain(CoaConfigurationMappingJpaEntity entity) {
        return new CoaConfigurationMapping(
                entity.getId(),
                entity.getTenantId(),
                entity.getProfileId(),
                entity.getCoaFieldId(),
                entity.getAccountId(),
                entity.getMandatoryOverride(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
