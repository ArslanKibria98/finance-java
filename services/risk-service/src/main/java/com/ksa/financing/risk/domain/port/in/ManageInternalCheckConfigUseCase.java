package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.InternalCheckConfig;

import java.util.List;
import java.util.UUID;

public interface ManageInternalCheckConfigUseCase {
    List<InternalCheckConfig> listConfigs(UUID tenantId);
    InternalCheckConfig updateConfig(UUID tenantId, UUID id, UpdateConfigCommand command);

    record UpdateConfigCommand(
            @com.fasterxml.jackson.annotation.JsonProperty("active") Boolean active,
            @com.fasterxml.jackson.annotation.JsonProperty("blockCodeId") java.util.UUID blockCodeId) {}
}
