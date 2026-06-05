package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.InternalCheckConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InternalCheckConfigRepository {
    List<InternalCheckConfig> findAll(UUID tenantId);
    Optional<InternalCheckConfig> findByCheckName(UUID tenantId, String checkName);
    Optional<InternalCheckConfig> findById(UUID tenantId, UUID id);
    InternalCheckConfig save(InternalCheckConfig config);
}
