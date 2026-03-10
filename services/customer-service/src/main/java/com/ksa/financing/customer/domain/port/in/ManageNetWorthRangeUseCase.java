package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.NetWorthRangeOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageNetWorthRangeUseCase {

    NetWorthRangeOption create(UUID tenantId, CreateNetWorthRangeCommand command);
    NetWorthRangeOption update(UUID tenantId, UUID id, UpdateNetWorthRangeCommand command);
    NetWorthRangeOption getById(UUID tenantId, UUID id);
    List<NetWorthRangeOption> getAll(UUID tenantId);
    List<NetWorthRangeOption> getActive(UUID tenantId);
    void deactivate(UUID tenantId, UUID id);

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
