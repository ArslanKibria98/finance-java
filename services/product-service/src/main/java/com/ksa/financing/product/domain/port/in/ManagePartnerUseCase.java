package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.Partner;

import java.util.List;
import java.util.UUID;

public interface ManagePartnerUseCase {

    Partner create(CreatePartnerCommand command);

    Partner getById(UUID tenantId, UUID partnerId);

    List<Partner> listByTenant(UUID tenantId);

    Partner update(UUID tenantId, UUID partnerId, UpdatePartnerCommand command);

    void activate(UUID tenantId, UUID partnerId);

    void deactivate(UUID tenantId, UUID partnerId);

    void suspend(UUID tenantId, UUID partnerId);

    record CreatePartnerCommand(
        UUID tenantId,
        String partnerCode,
        String nameEn,
        String nameAr,
        String email,
        String phone,
        String contactPerson,
        String logoUrl,
        UUID createdBy
    ) {}

    record UpdatePartnerCommand(
        String nameEn,
        String nameAr,
        String email,
        String phone,
        String contactPerson,
        String logoUrl,
        UUID updatedBy
    ) {}
}
