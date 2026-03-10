package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import java.util.List;
import java.util.UUID;

public interface ManageCategoryUseCase {

    // Master Categories
    List<MasterCategory> listMasterCategories(UUID tenantId);
    MasterCategory getMasterCategory(UUID tenantId, UUID id);
    MasterCategory createMasterCategory(UUID tenantId, String code, String nameEn, String nameAr,
                                        String descriptionEn, String descriptionAr, String iconUrl, int sortOrder);
    MasterCategory updateMasterCategory(UUID tenantId, UUID id, String nameEn, String nameAr,
                                        String descriptionEn, String descriptionAr, String iconUrl,
                                        int sortOrder, boolean active);
    void deleteMasterCategory(UUID tenantId, UUID id);

    // Sub-Categories
    List<SubCategory> listSubCategories(UUID tenantId, UUID masterCategoryId);
    SubCategory createSubCategory(UUID tenantId, UUID masterCategoryId, String code,
                                  String nameEn, String nameAr, int sortOrder);
    SubCategory updateSubCategory(UUID tenantId, UUID id, String nameEn, String nameAr,
                                  int sortOrder, boolean active);
    void deleteSubCategory(UUID tenantId, UUID id);
}
