package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.tenant.TenantConfig;

import java.util.List;
import java.util.UUID;

public interface ManageTenantConfigUseCase {

    TenantConfig create(UUID tenantId, CreateTenantConfigCommand command);

    TenantConfig update(UUID tenantId, UUID id, UpdateTenantConfigCommand command);

    TenantConfig getById(UUID tenantId, UUID id);

    TenantConfig getByTenantId(UUID tenantId);

    List<TenantConfig> getAll();

    void activate(UUID tenantId, UUID id);

    void deactivate(UUID tenantId, UUID id);

    record CreateTenantConfigCommand(
            String tenantName,
            String tenantNameAr,
            boolean customerRiskEnabled,
            boolean businessRiskEnabled,
            boolean loanRiskEnabled
    ) {}

    record UpdateTenantConfigCommand(
            String tenantName,
            String tenantNameAr,
            Boolean customerRiskEnabled,
            Boolean businessRiskEnabled,
            Boolean loanRiskEnabled
    ) {}
}
