package com.ksa.financing.ledger.domain.port.in;

import com.ksa.financing.ledger.domain.model.CoaFieldLov;

import java.util.List;
import java.util.UUID;

public interface ManageCoaFieldLovUseCase {

    CoaFieldLov create(CreateCoaFieldLovCommand command);

    CoaFieldLov update(UUID tenantId, UUID fieldLovId, UpdateCoaFieldLovCommand command);

    CoaFieldLov getById(UUID tenantId, UUID fieldLovId);

    List<CoaFieldLov> list(UUID tenantId, boolean activeOnly);

    CoaFieldLov deactivate(UUID tenantId, UUID fieldLovId);

    CoaFieldLov activate(UUID tenantId, UUID fieldLovId);

    record CreateCoaFieldLovCommand(
            UUID tenantId,
            String fieldKey,
            String fieldLabelEn,
            String fieldLabelAr,
            String category,
            boolean mandatoryDefault,
            int displayOrder
    ) {}

    record UpdateCoaFieldLovCommand(
            String fieldLabelEn,
            String fieldLabelAr,
            String category,
            boolean mandatoryDefault,
            int displayOrder
    ) {}
}
