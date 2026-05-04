package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import java.util.UUID;

public interface ManageCategoryUseCase {

    // Master Categories
    PageResponse<MasterCategory> listMasterCategories(UUID tenantId, PageQuery pageQuery);
    MasterCategory getMasterCategory(UUID tenantId, UUID id);
    MasterCategory createMasterCategory(UUID tenantId, String code, String nameEn, String nameAr,
                                        String descriptionEn, String descriptionAr, String iconUrl, int sortOrder);
    MasterCategory updateMasterCategory(UUID tenantId, UUID id, String nameEn, String nameAr,
                                        String descriptionEn, String descriptionAr, String iconUrl,
                                        int sortOrder, boolean active);
    void deleteMasterCategory(UUID tenantId, UUID id);
    MasterCategory activateMasterCategory(UUID tenantId, UUID id);
    MasterCategory deactivateMasterCategory(UUID tenantId, UUID id);

    // Sub-Categories
    PageResponse<SubCategory> listSubCategories(UUID tenantId, UUID masterCategoryId, PageQuery pageQuery);
    SubCategory createSubCategory(UUID tenantId, UUID masterCategoryId, String code,
                                  String nameEn, String nameAr, int sortOrder);
    SubCategory updateSubCategory(UUID tenantId, UUID id, String nameEn, String nameAr,
                                  int sortOrder, boolean active);
    void deleteSubCategory(UUID tenantId, UUID id);
    SubCategory activateSubCategory(UUID tenantId, UUID id);
    SubCategory deactivateSubCategory(UUID tenantId, UUID id);
}
