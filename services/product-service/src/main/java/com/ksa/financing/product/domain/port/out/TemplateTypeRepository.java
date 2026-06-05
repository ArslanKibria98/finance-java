package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.product.domain.model.TemplateType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemplateTypeRepository {

    PageResponse<TemplateType> findAllByTenant(UUID tenantId, PageQuery pageQuery);
    List<TemplateType> findByCategory(UUID tenantId, String category);
    Optional<TemplateType> findById(UUID id);
    Optional<TemplateType> findByNameAndCategory(UUID tenantId, String nameEn, String category);
    TemplateType save(TemplateType type);
    void delete(UUID id);
}
