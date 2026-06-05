package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.blacklist.CountryBlacklistEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CountryBlacklistRepository {

    CountryBlacklistEntry save(CountryBlacklistEntry entry);

    Optional<CountryBlacklistEntry> findActiveByCountryCode(UUID tenantId, String countryCode);

    List<CountryBlacklistEntry> findAllActive(UUID tenantId);

    void deactivate(UUID tenantId, String countryCode);

    boolean isBlacklisted(UUID tenantId, String countryCode);
}
