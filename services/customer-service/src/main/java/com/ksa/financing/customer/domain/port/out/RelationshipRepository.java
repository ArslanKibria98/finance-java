package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.RelationshipOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface RelationshipRepository {
    RelationshipOption save(RelationshipOption option);
    Optional<RelationshipOption> findById(UUID tenantId, UUID id);
    PageResponse<RelationshipOption> findAllByTenantId(UUID tenantId, PageQuery pageQuery);
    PageResponse<RelationshipOption> findActiveByTenantId(UUID tenantId, PageQuery pageQuery);
    boolean existsByCode(UUID tenantId, String code);
    void softDelete(UUID tenantId, UUID id);
}
