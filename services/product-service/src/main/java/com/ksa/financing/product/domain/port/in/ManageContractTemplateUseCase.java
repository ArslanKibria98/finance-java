package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.ContractTemplate;
import java.util.List;
import java.util.UUID;

public interface ManageContractTemplateUseCase {

    List<ContractTemplate> listAll(UUID tenantId);
    List<ContractTemplate> listByProduct(UUID tenantId, UUID productId);
    List<ContractTemplate> listByType(UUID tenantId, UUID typeId);
    ContractTemplate getById(UUID tenantId, UUID id);
    ContractTemplate create(UUID tenantId, ContractTemplate template);
    ContractTemplate update(UUID tenantId, UUID id, ContractTemplate updates);
    void delete(UUID tenantId, UUID id);
}
