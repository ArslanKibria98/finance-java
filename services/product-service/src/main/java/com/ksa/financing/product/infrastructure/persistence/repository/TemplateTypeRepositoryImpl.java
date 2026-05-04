package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.product.domain.model.TemplateType;
import com.ksa.financing.product.domain.port.out.TemplateTypeRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.TemplateTypeJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.mapper.TemplateTypePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TemplateTypeRepositoryImpl implements TemplateTypeRepository {

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("category", "isActive", "nameEn", "nameAr");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("nameEn", "nameAr", "category");

    private final JpaTemplateTypeRepository jpaTemplateTypeRepository;

    @Override
    public PageResponse<TemplateType> findAllByTenant(UUID tenantId, PageQuery pageQuery) {
        Specification<TemplateTypeJpaEntity> base = (root, query, cb) ->
                cb.and(cb.equal(root.get("tenantId"), tenantId), cb.equal(root.get("isActive"), true));

        Specification<TemplateTypeJpaEntity> dynamic = SpecificationBuilder.<TemplateTypeJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<TemplateTypeJpaEntity> page = jpaTemplateTypeRepository.findAll(base.and(dynamic), pageQuery.toPageable());
        return PageResponse.from(page, TemplateTypePersistenceMapper::toDomain);
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
