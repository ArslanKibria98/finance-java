package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningPolicyId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DunningPolicyRepository {

    DunningPolicy save(DunningPolicy policy);

    Optional<DunningPolicy> findById(UUID tenantId, DunningPolicyId id);

    Optional<DunningPolicy> findByName(UUID tenantId, String policyName);

    /** Active policy bound to a specific product code. */
    Optional<DunningPolicy> findActiveByProductCode(UUID tenantId, String productCode);

    /** Tenant-wide active default policy (product_code IS NULL AND is_default=true). */
    Optional<DunningPolicy> findActiveDefault(UUID tenantId);

    List<DunningPolicy> findAll(UUID tenantId);

    List<DunningPolicy> findAllActive(UUID tenantId);

    /**
     * Clear the default flag on any other policy for this tenant.
     * Used before marking a new policy as default to ensure the partial unique index holds.
     */
    void clearDefaultExcept(UUID tenantId, DunningPolicyId keepId);

    void deleteById(UUID tenantId, DunningPolicyId id);

    boolean existsByName(UUID tenantId, String policyName);
}
