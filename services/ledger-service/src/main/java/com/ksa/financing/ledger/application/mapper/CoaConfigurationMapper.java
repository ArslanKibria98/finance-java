package com.ksa.financing.ledger.application.mapper;

import com.ksa.financing.ledger.application.dto.*;
import com.ksa.financing.ledger.domain.model.CoaConfigurationMapping;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;
import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import org.springframework.stereotype.Component;

@Component
public class CoaConfigurationMapper {

    public CoaFieldLovResponse toResponse(CoaFieldLov model) {
        return new CoaFieldLovResponse(
                model.getId(),
                model.getTenantId(),
                model.getFieldKey(),
                model.getFieldLabelEn(),
                model.getFieldLabelAr(),
                model.getCategory(),
                model.isMandatoryDefault(),
                model.getDisplayOrder(),
                model.getStatus().name(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }

    public CoaConfigurationProfileResponse toResponse(CoaConfigurationProfile model) {
        return new CoaConfigurationProfileResponse(
                model.getId(),
                model.getTenantId(),
                model.getProductCode(),
                model.getProfileName(),
                model.getStatus().name(),
                model.getEffectiveFrom(),
                model.getEffectiveTo(),
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }

    public CoaConfigurationMappingResponse toResponse(CoaConfigurationMapping model) {
        return toResponse(model, null);
    }

    public CoaConfigurationMappingResponse toResponse(CoaConfigurationMapping model, java.util.UUID productId) {
        return new CoaConfigurationMappingResponse(
                model.id(),
                model.tenantId(),
                productId,
                model.profileId(),
                model.coaFieldId(),
                model.accountId(),
                model.mandatoryOverride(),
                model.notes(),
                model.createdAt(),
                model.updatedAt()
        );
    }

    public CoaConfigurationValidationResponse toResponse(
            com.ksa.financing.ledger.domain.port.in.ManageCoaConfigurationUseCase.ValidationResult validation) {
        return new CoaConfigurationValidationResponse(
                validation.valid(),
                validation.missingMandatoryFieldKeys(),
                validation.invalidAccountCodes()
        );
    }
}
