package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import com.ksa.financing.product.domain.port.in.ManageCategoryUseCase;
import com.ksa.financing.product.domain.port.out.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageCategoryUseCaseImpl implements ManageCategoryUseCase {

    private final CategoryRepository categoryRepository;

    // --- Master Categories ---

    @Override
    @Transactional(readOnly = true)
    public List<MasterCategory> listMasterCategories(UUID tenantId) {
        log.debug("Listing master categories for tenant: {}", tenantId);
        return categoryRepository.findAllMasterCategories(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public MasterCategory getMasterCategory(UUID tenantId, UUID id) {
        log.debug("Getting master category id={} for tenant={}", id, tenantId);
        return categoryRepository.findMasterCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("MasterCategory", id.toString()));
    }

    @Override
    @Transactional
    public MasterCategory createMasterCategory(UUID tenantId, String code, String nameEn, String nameAr,
                                                String descriptionEn, String descriptionAr, String iconUrl, int sortOrder) {
        log.info("Creating master category code={} for tenant={}", code, tenantId);

        categoryRepository.findMasterCategoryByCode(tenantId, code).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.Product.DUPLICATE_CODE,
                    "Master category with code already exists: " + code, code);
        });

        var category = new MasterCategory();
        category.setId(UUID.randomUUID());
        category.setTenantId(tenantId);
        category.setCode(code.toUpperCase());
        category.setNameEn(nameEn);
        category.setNameAr(nameAr);
        category.setDescriptionEn(descriptionEn);
        category.setDescriptionAr(descriptionAr);
        category.setIconUrl(iconUrl);
        category.setSortOrder(sortOrder);
        category.setActive(true);
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());

        return categoryRepository.saveMasterCategory(category);
    }

    @Override
    @Transactional
    public MasterCategory updateMasterCategory(UUID tenantId, UUID id, String nameEn, String nameAr,
                                                String descriptionEn, String descriptionAr, String iconUrl,
                                                int sortOrder, boolean active) {
        log.info("Updating master category id={} for tenant={}", id, tenantId);

        var category = categoryRepository.findMasterCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("MasterCategory", id.toString()));

        category.setNameEn(nameEn);
        category.setNameAr(nameAr);
        category.setDescriptionEn(descriptionEn);
        category.setDescriptionAr(descriptionAr);
        category.setIconUrl(iconUrl);
        category.setSortOrder(sortOrder);
        category.setActive(active);
        category.setUpdatedAt(Instant.now());

        return categoryRepository.saveMasterCategory(category);
    }

    @Override
    @Transactional
    public void deleteMasterCategory(UUID tenantId, UUID id) {
        log.info("Deleting master category id={} for tenant={}", id, tenantId);
        categoryRepository.findMasterCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("MasterCategory", id.toString()));
        categoryRepository.deleteMasterCategory(id);
    }

    @Override
    @Transactional
    public MasterCategory activateMasterCategory(UUID tenantId, UUID id) {
        log.info("Activating master category id={} for tenant={}", id, tenantId);
        var category = categoryRepository.findMasterCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("MasterCategory", id.toString()));
        category.setActive(true);
        category.setUpdatedAt(Instant.now());
        return categoryRepository.saveMasterCategory(category);
    }

    @Override
    @Transactional
    public MasterCategory deactivateMasterCategory(UUID tenantId, UUID id) {
        log.info("Deactivating master category id={} for tenant={}", id, tenantId);
        var category = categoryRepository.findMasterCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("MasterCategory", id.toString()));
        category.setActive(false);
        category.setUpdatedAt(Instant.now());
        return categoryRepository.saveMasterCategory(category);
    }

    // --- Sub-Categories ---

    @Override
    @Transactional(readOnly = true)
    public List<SubCategory> listSubCategories(UUID tenantId, UUID masterCategoryId) {
        log.debug("Listing sub-categories for master: {} tenant: {}", masterCategoryId, tenantId);
        return categoryRepository.findSubCategoriesByMaster(tenantId, masterCategoryId);
    }

    @Override
    @Transactional
    public SubCategory createSubCategory(UUID tenantId, UUID masterCategoryId, String code,
                                          String nameEn, String nameAr, int sortOrder) {
        log.info("Creating sub-category code={} for master={} tenant={}", code, masterCategoryId, tenantId);

        categoryRepository.findMasterCategoryById(masterCategoryId)
                .orElseThrow(() -> NotFoundException.forEntity("MasterCategory", masterCategoryId.toString()));

        categoryRepository.findSubCategoryByCode(masterCategoryId, code).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.Product.DUPLICATE_CODE,
                    "Sub-category with code already exists under this master: " + code, code);
        });

        var subCategory = new SubCategory();
        subCategory.setId(UUID.randomUUID());
        subCategory.setTenantId(tenantId);
        subCategory.setMasterCategoryId(masterCategoryId);
        subCategory.setCode(code.toUpperCase());
        subCategory.setNameEn(nameEn);
        subCategory.setNameAr(nameAr);
        subCategory.setSortOrder(sortOrder);
        subCategory.setActive(true);
        subCategory.setCreatedAt(Instant.now());
        subCategory.setUpdatedAt(Instant.now());

        return categoryRepository.saveSubCategory(subCategory);
    }

    @Override
    @Transactional
    public SubCategory updateSubCategory(UUID tenantId, UUID id, String nameEn, String nameAr,
                                          int sortOrder, boolean active) {
        log.info("Updating sub-category id={} for tenant={}", id, tenantId);

        var subCategory = categoryRepository.findSubCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("SubCategory", id.toString()));

        subCategory.setNameEn(nameEn);
        subCategory.setNameAr(nameAr);
        subCategory.setSortOrder(sortOrder);
        subCategory.setActive(active);
        subCategory.setUpdatedAt(Instant.now());

        return categoryRepository.saveSubCategory(subCategory);
    }

    @Override
    @Transactional
    public void deleteSubCategory(UUID tenantId, UUID id) {
        log.info("Deleting sub-category id={} for tenant={}", id, tenantId);
        categoryRepository.findSubCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("SubCategory", id.toString()));
        categoryRepository.deleteSubCategory(id);
    }

    @Override
    @Transactional
    public SubCategory activateSubCategory(UUID tenantId, UUID id) {
        log.info("Activating sub-category id={} for tenant={}", id, tenantId);
        var subCategory = categoryRepository.findSubCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("SubCategory", id.toString()));
        subCategory.setActive(true);
        subCategory.setUpdatedAt(Instant.now());
        return categoryRepository.saveSubCategory(subCategory);
    }

    @Override
    @Transactional
    public SubCategory deactivateSubCategory(UUID tenantId, UUID id) {
        log.info("Deactivating sub-category id={} for tenant={}", id, tenantId);
        var subCategory = categoryRepository.findSubCategoryById(id)
                .orElseThrow(() -> NotFoundException.forEntity("SubCategory", id.toString()));
        subCategory.setActive(false);
        subCategory.setUpdatedAt(Instant.now());
        return categoryRepository.saveSubCategory(subCategory);
    }
}
