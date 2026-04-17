package com.ksa.financing.product.application.mapper;

// TODO [ARCH-VIOLATION] Application layer imports adapter layer DTOs.
// Fix: Move CategoryResponse/SubCategoryResponse to application.dto package,
// or create application-layer DTOs and map to adapter responses in the controller.
import com.ksa.financing.product.adapter.rest.response.CategoryResponse;
import com.ksa.financing.product.adapter.rest.response.SubCategoryResponse;
import com.ksa.financing.product.domain.model.MasterCategory;
import com.ksa.financing.product.domain.model.SubCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    private CategoryMapper() {}

    public static CategoryResponse toResponse(MasterCategory category) {
        if (category == null) {
            return null;
        }

        return new CategoryResponse(
                category.getId(),
                category.getCode(),
                category.getNameEn(),
                category.getNameAr(),
                category.getDescriptionEn(),
                category.getDescriptionAr(),
                category.getIconUrl(),
                category.getSortOrder(),
                category.isActive()
        );
    }

    public static SubCategoryResponse toSubCategoryResponse(SubCategory subCategory) {
        if (subCategory == null) {
            return null;
        }

        return new SubCategoryResponse(
                subCategory.getId(),
                subCategory.getMasterCategoryId(),
                subCategory.getCode(),
                subCategory.getNameEn(),
                subCategory.getNameAr(),
                subCategory.getSortOrder(),
                subCategory.isActive()
        );
    }
}
