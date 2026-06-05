package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.blacklist.IbanBlacklistEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IbanBlacklistRepository {

    IbanBlacklistEntry save(IbanBlacklistEntry entry);

    Optional<IbanBlacklistEntry> findActiveByIbanHash(UUID tenantId, String ibanHash);

    List<IbanBlacklistEntry> findAllActive(UUID tenantId);

    void deactivate(UUID tenantId, String ibanHash);

    boolean isBlacklisted(UUID tenantId, String ibanHash);
}
