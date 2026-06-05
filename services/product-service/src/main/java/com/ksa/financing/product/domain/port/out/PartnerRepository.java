package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.Partner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository {

    Partner save(Partner partner);

    Optional<Partner> findById(UUID tenantId, UUID partnerId);

    List<Partner> findAllByTenant(UUID tenantId);

    boolean existsByCode(UUID tenantId, String partnerCode);
}
