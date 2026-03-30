package com.ksa.financing.product.application.mapper;

import com.ksa.financing.product.adapter.rest.response.ContractTemplateResponse;
import com.ksa.financing.product.domain.model.ContractTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContractTemplateMapper {

    private ContractTemplateMapper() {}

    public static ContractTemplateResponse toResponse(ContractTemplate t) {
        if (t == null) return null;

        return new ContractTemplateResponse(
                t.getId(),
                t.getNameEn(),
                t.getNameAr(),
                t.getProductId(),
                t.getProductNameEn(),
                t.getProductNameAr(),
                t.getTypeId(),
                t.getTypeNameEn(),
                t.getTypeNameAr(),
                t.getLanguage(),
                t.getMessage(),
                t.isActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
