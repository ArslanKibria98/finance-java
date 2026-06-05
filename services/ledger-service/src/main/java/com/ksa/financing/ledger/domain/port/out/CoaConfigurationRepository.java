package com.ksa.financing.ledger.domain.port.out;

import com.ksa.financing.ledger.domain.model.CoaConfigurationMapping;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoaConfigurationRepository {

    CoaConfigurationProfile saveProfile(CoaConfigurationProfile profile);

    Optional<CoaConfigurationProfile> findProfileById(UUID tenantId, UUID profileId);

    List<CoaConfigurationProfile> findProfilesByProductCode(UUID tenantId, String productCode);

    List<CoaConfigurationMapping> findMappingsByProfileId(UUID tenantId, UUID profileId);

    void replaceMappings(UUID tenantId, UUID profileId, List<CoaConfigurationMapping> mappings);
}
