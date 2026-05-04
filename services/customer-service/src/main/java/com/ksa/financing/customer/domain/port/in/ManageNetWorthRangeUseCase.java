package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.NetWorthRangeOption;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface ManageNetWorthRangeUseCase {

    NetWorthRangeOption create(UUID tenantId, CreateNetWorthRangeCommand command);
    NetWorthRangeOption update(UUID tenantId, UUID id, UpdateNetWorthRangeCommand command);
    NetWorthRangeOption getById(UUID tenantId, UUID id);
    PageResponse<NetWorthRangeOption> getAll(UUID tenantId, PageQuery pageQuery);
    PageResponse<NetWorthRangeOption> getActive(UUID tenantId, PageQuery pageQuery);
    void deactivate(UUID tenantId, UUID id);
    void activate(UUID tenantId, UUID id);
    void delete(UUID tenantId, UUID id);

    record CreateNetWorthRangeCommand(
        String code,
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        BigDecimal minValue,
        BigDecimal maxValue,
        int displayOrder
    ) {}

    record UpdateNetWorthRangeCommand(
        String nameEn,
        String nameAr,
        String descriptionEn,
        String descriptionAr,
        BigDecimal minValue,
        BigDecimal maxValue,
        Boolean isActive,
        Integer displayOrder
    ) {}
}
