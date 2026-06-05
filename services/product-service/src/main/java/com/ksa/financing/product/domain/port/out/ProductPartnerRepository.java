package com.ksa.financing.product.domain.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductPartnerRepository {
    void saveAffiliation(UUID tenantId, UUID productId, UUID partnerId,
                         String affiliationType, BigDecimal commissionPercentage);
    void deleteAffiliation(UUID productId, UUID partnerId);
    boolean existsAffiliation(UUID productId, UUID partnerId);
}
