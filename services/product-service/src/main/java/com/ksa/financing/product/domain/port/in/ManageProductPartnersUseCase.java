package com.ksa.financing.product.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface ManageProductPartnersUseCase {

    void addPartnerAffiliation(UUID tenantId, UUID productId, AddPartnerAffiliationCommand command);
    void removePartnerAffiliation(UUID tenantId, UUID productId, UUID partnerId);

    record AddPartnerAffiliationCommand(
        UUID partnerId,
        String affiliationType,
        BigDecimal commissionPercentage
    ) {}
}
