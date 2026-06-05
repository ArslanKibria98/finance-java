package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.ContractTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractTemplateRepository {

    List<ContractTemplate> findAllByTenant(UUID tenantId);
    List<ContractTemplate> findByProduct(UUID tenantId, UUID productId);
    List<ContractTemplate> findByType(UUID tenantId, UUID typeId);
    Optional<ContractTemplate> findById(UUID id);
    ContractTemplate save(ContractTemplate template);
    void delete(UUID id);
}
