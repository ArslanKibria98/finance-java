package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    // Master Categories
    List<MasterCategory> findAllMasterCategories(UUID tenantId);
    PageResponse<MasterCategory> findAllMasterCategories(UUID tenantId, PageQuery pageQuery);
    Optional<MasterCategory> findMasterCategoryById(UUID id);
    Optional<MasterCategory> findMasterCategoryByCode(UUID tenantId, String code);
    MasterCategory saveMasterCategory(MasterCategory category);
    void deleteMasterCategory(UUID id);

    // Sub-Categories
    List<SubCategory> findSubCategoriesByMaster(UUID tenantId, UUID masterCategoryId);
    PageResponse<SubCategory> findSubCategoriesByMaster(UUID tenantId, UUID masterCategoryId, PageQuery pageQuery);
    Optional<SubCategory> findSubCategoryById(UUID id);
    Optional<SubCategory> findSubCategoryByCode(UUID masterCategoryId, String code);
    SubCategory saveSubCategory(SubCategory subCategory);
    void deleteSubCategory(UUID id);
}
