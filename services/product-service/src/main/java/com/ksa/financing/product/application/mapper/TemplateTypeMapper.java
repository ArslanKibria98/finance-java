package com.ksa.financing.product.application.mapper;

import com.ksa.financing.product.adapter.rest.response.TemplateTypeResponse;
import com.ksa.financing.product.domain.model.TemplateType;
import org.springframework.stereotype.Component;

@Component
public class TemplateTypeMapper {

    private TemplateTypeMapper() {}

    public static TemplateTypeResponse toResponse(TemplateType type) {
        if (type == null) return null;

        return new TemplateTypeResponse(
                type.getId(),
                type.getNameEn(),
                type.getNameAr(),
                type.getCategory(),
                type.isActive(),
                type.getCreatedAt(),
                type.getUpdatedAt()
        );
    }
}
