package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;
import com.ksa.financing.lending.domain.port.out.PurposeOfFinanceRepository;
import com.ksa.financing.lending.infrastructure.persistence.entity.PurposeOfFinanceJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PurposeOfFinanceRepositoryImpl implements PurposeOfFinanceRepository {

    private final JpaPurposeOfFinanceRepository jpaRepository;

    @Override
    @Transactional
    public PurposeOfFinanceEntry save(PurposeOfFinanceEntry entry) {
        var entity = toEntity(entry);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<PurposeOfFinanceEntry> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id).map(this::toDomain);
    }

    @Override
    public Optional<PurposeOfFinanceEntry> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByTenantIdAndCode(tenantId, code).map(this::toDomain);
    }

    @Override
    public List<PurposeOfFinanceEntry> findAllActive(UUID tenantId) {
        return jpaRepository.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<PurposeOfFinanceEntry> findAll(UUID tenantId) {
        return jpaRepository.findByTenantIdOrderBySortOrder(tenantId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        jpaRepository.deleteByTenantIdAndId(tenantId, id);
    }

    private PurposeOfFinanceJpaEntity toEntity(PurposeOfFinanceEntry entry) {
        var entity = new PurposeOfFinanceJpaEntity();
        entity.setId(entry.getId());
        entity.setTenantId(entry.getTenantId());
        entity.setCode(entry.getCode());
        entity.setNameEn(entry.getNameEn());
        entity.setNameAr(entry.getNameAr());
        entity.setDescriptionEn(entry.getDescriptionEn());
        entity.setDescriptionAr(entry.getDescriptionAr());
        entity.setActive(entry.isActive());
        entity.setSortOrder(entry.getSortOrder());
        entity.setCreatedAt(entry.getCreatedAt());
        entity.setUpdatedAt(entry.getUpdatedAt());
        entity.setCreatedBy(entry.getCreatedBy());
        entity.setVersion(entry.getVersion());
        return entity;
    }

    private PurposeOfFinanceEntry toDomain(PurposeOfFinanceJpaEntity entity) {
        return PurposeOfFinanceEntry.reconstitute(
                entity.getId(), entity.getTenantId(), entity.getCode(),
                entity.getNameEn(), entity.getNameAr(),
                entity.getDescriptionEn(), entity.getDescriptionAr(),
                entity.isActive(), entity.getSortOrder(),
                entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getCreatedBy(), entity.getVersion()
        );
    }
}
