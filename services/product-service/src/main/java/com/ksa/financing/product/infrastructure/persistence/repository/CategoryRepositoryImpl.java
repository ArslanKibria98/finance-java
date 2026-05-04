package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import com.ksa.financing.product.domain.port.out.CategoryRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.MasterCategoryJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.entity.SubCategoryJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.mapper.CategoryPersistenceMapper;
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
public class CategoryRepositoryImpl implements CategoryRepository {

    private static final Set<String> MASTER_ALLOWED_FILTER_FIELDS = Set.of(
            "code", "nameEn", "nameAr", "isActive"
    );

    private static final Set<String> MASTER_SEARCHABLE_FIELDS = Set.of(
            "code", "nameEn", "nameAr", "descriptionEn", "descriptionAr"
    );

    private static final Set<String> SUB_ALLOWED_FILTER_FIELDS = Set.of(
            "code", "nameEn", "nameAr", "isActive", "masterCategoryId"
    );

    private static final Set<String> SUB_SEARCHABLE_FIELDS = Set.of(
            "code", "nameEn", "nameAr"
    );

    private final JpaMasterCategoryRepository jpaMasterCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;

    // --- Master Categories ---

    @Override
    public List<MasterCategory> findAllMasterCategories(UUID tenantId) {
        log.debug("Listing active master categories for tenantId={}", tenantId);
        return jpaMasterCategoryRepository.findAllByTenantId(tenantId)
                .stream()
                .map(CategoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<MasterCategory> findAllMasterCategories(UUID tenantId, PageQuery pageQuery) {
        log.debug("Listing master categories with pagination for tenantId={}", tenantId);

        Specification<MasterCategoryJpaEntity> tenantSpec = (root, query, cb) ->
                cb.equal(root.get("tenantId"), tenantId);

        Specification<MasterCategoryJpaEntity> dynamic = SpecificationBuilder.<MasterCategoryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(MASTER_ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(MASTER_SEARCHABLE_FIELDS)
                .build();

        Page<MasterCategoryJpaEntity> page = jpaMasterCategoryRepository.findAll(
                tenantSpec.and(dynamic),
                pageQuery.toPageable());

        List<MasterCategory> content = page.getContent().stream()
                .map(CategoryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
    }

    @Override
    public Optional<MasterCategory> findMasterCategoryById(UUID id) {
        log.debug("Finding master category by id={}", id);
        return jpaMasterCategoryRepository.findById(id)
                .map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public Optional<MasterCategory> findMasterCategoryByCode(UUID tenantId, String code) {
        log.debug("Finding master category by code={}, tenantId={}", code, tenantId);
        return jpaMasterCategoryRepository.findByTenantIdAndCode(tenantId, code)
                .map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public MasterCategory saveMasterCategory(MasterCategory category) {
        log.debug("Saving master category: code={}", category.getCode());
        MasterCategoryJpaEntity entity;
        if (category.getId() != null) {
            entity = jpaMasterCategoryRepository.findById(category.getId())
                    .orElseGet(MasterCategoryJpaEntity::new);
        } else {
            entity = new MasterCategoryJpaEntity();
        }
        entity.setTenantId(category.getTenantId());
        entity.setCode(category.getCode());
        entity.setNameEn(category.getNameEn());
        entity.setNameAr(category.getNameAr());
        entity.setDescriptionEn(category.getDescriptionEn());
        entity.setDescriptionAr(category.getDescriptionAr());
        entity.setIconUrl(category.getIconUrl());
        entity.setSortOrder(category.getSortOrder());
        entity.setActive(category.isActive());
        entity.setCreatedAt(category.getCreatedAt() != null
                ? category.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : java.time.OffsetDateTime.now());
        entity.setUpdatedAt(category.getUpdatedAt() != null
                ? category.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : java.time.OffsetDateTime.now());
        var saved = jpaMasterCategoryRepository.save(entity);
        return CategoryPersistenceMapper.toDomain(saved);
    }

    @Override
    public void deleteMasterCategory(UUID id) {
        log.debug("Deleting master category id={}", id);
        jpaMasterCategoryRepository.deleteById(id);
    }

    // --- Sub-Categories ---

    @Override
    public List<SubCategory> findSubCategoriesByMaster(UUID tenantId, UUID masterCategoryId) {
        log.debug("Listing active sub-categories for masterCategoryId={}, tenantId={}", masterCategoryId, tenantId);
        return jpaSubCategoryRepository
                .findAllByMasterCategoryIdAndTenantId(masterCategoryId, tenantId)
                .stream()
                .map(CategoryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<SubCategory> findSubCategoriesByMaster(UUID tenantId, UUID masterCategoryId, PageQuery pageQuery) {
        log.debug("Listing sub-categories with pagination for masterCategoryId={}, tenantId={}", masterCategoryId, tenantId);

        Specification<SubCategoryJpaEntity> masterAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("masterCategoryId"), masterCategoryId)
                );

        Specification<SubCategoryJpaEntity> dynamic = SpecificationBuilder.<SubCategoryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(SUB_ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SUB_SEARCHABLE_FIELDS)
                .build();

        Page<SubCategoryJpaEntity> page = jpaSubCategoryRepository.findAll(
                masterAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<SubCategory> content = page.getContent().stream()
                .map(CategoryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
    }

    @Override
    public Optional<SubCategory> findSubCategoryById(UUID id) {
        log.debug("Finding sub-category by id={}", id);
        return jpaSubCategoryRepository.findById(id)
                .map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public Optional<SubCategory> findSubCategoryByCode(UUID masterCategoryId, String code) {
        log.debug("Finding sub-category by code={}, masterCategoryId={}", code, masterCategoryId);
        return jpaSubCategoryRepository.findByMasterCategoryIdAndCode(masterCategoryId, code)
                .map(CategoryPersistenceMapper::toDomain);
    }

    @Override
    public SubCategory saveSubCategory(SubCategory subCategory) {
        log.debug("Saving sub-category: code={}", subCategory.getCode());
        SubCategoryJpaEntity entity;
        if (subCategory.getId() != null) {
            entity = jpaSubCategoryRepository.findById(subCategory.getId())
                    .orElseGet(SubCategoryJpaEntity::new);
        } else {
            entity = new SubCategoryJpaEntity();
        }
        entity.setTenantId(subCategory.getTenantId());
        entity.setMasterCategoryId(subCategory.getMasterCategoryId());
        entity.setCode(subCategory.getCode());
        entity.setNameEn(subCategory.getNameEn());
        entity.setNameAr(subCategory.getNameAr());
        entity.setSortOrder(subCategory.getSortOrder());
        entity.setActive(subCategory.isActive());
        entity.setCreatedAt(subCategory.getCreatedAt() != null
                ? subCategory.getCreatedAt().atOffset(java.time.ZoneOffset.UTC) : java.time.OffsetDateTime.now());
        entity.setUpdatedAt(subCategory.getUpdatedAt() != null
                ? subCategory.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC) : java.time.OffsetDateTime.now());
        var saved = jpaSubCategoryRepository.save(entity);
        return CategoryPersistenceMapper.toDomain(saved);
    }

    @Override
    public void deleteSubCategory(UUID id) {
        log.debug("Deleting sub-category id={}", id);
        jpaSubCategoryRepository.deleteById(id);
    }
}
