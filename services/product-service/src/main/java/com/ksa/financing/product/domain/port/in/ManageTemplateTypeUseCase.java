package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.TemplateType;
import java.util.List;
import java.util.UUID;

public interface ManageTemplateTypeUseCase {

    List<TemplateType> listAll(UUID tenantId);
    List<TemplateType> listByCategory(UUID tenantId, String category);
    TemplateType getById(UUID tenantId, UUID id);
    TemplateType create(UUID tenantId, TemplateType type);
    TemplateType update(UUID tenantId, UUID id, TemplateType updates);
    void delete(UUID tenantId, UUID id);
}
