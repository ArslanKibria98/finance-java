package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import com.ksa.financing.product.domain.port.out.CategoryRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.MasterCategoryJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.entity.SubCategoryJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.mapper.CategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CategoryRepositoryImpl implements CategoryRepository {

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
