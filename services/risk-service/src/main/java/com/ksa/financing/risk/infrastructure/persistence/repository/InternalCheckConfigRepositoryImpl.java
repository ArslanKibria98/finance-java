package com.ksa.financing.risk.infrastructure.persistence.repository;

import com.ksa.financing.risk.domain.model.InternalCheckConfig;
import com.ksa.financing.risk.domain.port.out.InternalCheckConfigRepository;
import com.ksa.financing.risk.infrastructure.persistence.entity.InternalCheckConfigJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InternalCheckConfigRepositoryImpl implements InternalCheckConfigRepository {

    private final JpaInternalCheckConfigRepository jpaRepository;

    @Override
    public List<InternalCheckConfig> findAll(UUID tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InternalCheckConfig> findByCheckName(UUID tenantId, String checkName) {
        return jpaRepository.findByTenantIdAndCheckName(tenantId, checkName).map(this::toDomain);
    }

    @Override
    public Optional<InternalCheckConfig> findById(UUID tenantId, UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public InternalCheckConfig save(InternalCheckConfig config) {
        InternalCheckConfigJpaEntity entity = toEntity(config);
        return toDomain(jpaRepository.save(entity));
    }

    private InternalCheckConfig toDomain(InternalCheckConfigJpaEntity entity) {
        return InternalCheckConfig.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .checkName(entity.getCheckName())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .active(entity.isActive())
                .blockCodeId(entity.getBlockCodeId())
                .createdAt(entity.getCreatedAt().toInstant())
                .updatedAt(entity.getUpdatedAt().toInstant())
                .version(entity.getVersion())
                .build();
    }

    private InternalCheckConfigJpaEntity toEntity(InternalCheckConfig domain) {
        InternalCheckConfigJpaEntity entity = new InternalCheckConfigJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCheckName(domain.getCheckName());
        entity.setDisplayName(domain.getDisplayName());
        entity.setDescription(domain.getDescription());
        entity.setActive(domain.isActive());
        entity.setBlockCodeId(domain.getBlockCodeId());
        entity.setCreatedAt(domain.getCreatedAt().atOffset(ZoneOffset.UTC));
        entity.setUpdatedAt(domain.getUpdatedAt().atOffset(ZoneOffset.UTC));
        entity.setVersion(domain.getVersion());
        return entity;
    }
}
