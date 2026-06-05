package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.status.EntityStatusRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityStatusRepository {

    EntityStatusRecord save(EntityStatusRecord record);

    Optional<EntityStatusRecord> findLatestByEntityReference(UUID tenantId, String entityReference);

    List<EntityStatusRecord> findByEntityReference(UUID tenantId, String entityReference);

    Optional<EntityStatusRecord> findById(UUID tenantId, UUID id);
}
