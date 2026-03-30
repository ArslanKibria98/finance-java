package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.TemplateType;
import com.ksa.financing.product.domain.port.out.TemplateTypeRepository;
import com.ksa.financing.product.infrastructure.persistence.mapper.TemplateTypePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TemplateTypeRepositoryImpl implements TemplateTypeRepository {

    private final JpaTemplateTypeRepository jpaTemplateTypeRepository;

    @Override
    public List<TemplateType> findAllByTenant(UUID tenantId) {
        return jpaTemplateTypeRepository.findAllByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .map(TemplateTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<TemplateType> findByCategory(UUID tenantId, String category) {
        return jpaTemplateTypeRepository.findAllByTenantIdAndCategoryAndIsActiveTrue(tenantId, category)
                .stream()
                .map(TemplateTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TemplateType> findById(UUID id) {
        return jpaTemplateTypeRepository.findById(id)
                .map(TemplateTypePersistenceMapper::toDomain);
    }

    @Override
    public Optional<TemplateType> findByNameAndCategory(UUID tenantId, String nameEn, String category) {
        return jpaTemplateTypeRepository.findByTenantIdAndNameEnAndCategory(tenantId, nameEn, category)
                .map(TemplateTypePersistenceMapper::toDomain);
    }

    @Override
    public TemplateType save(TemplateType type) {
        var entity = TemplateTypePersistenceMapper.toEntity(type);
        var saved = jpaTemplateTypeRepository.save(entity);
        return TemplateTypePersistenceMapper.toDomain(saved);
    }

    @Override
    public void delete(UUID id) {
        jpaTemplateTypeRepository.deleteById(id);
    }
}
