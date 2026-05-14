package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.model.blacklist.CountryBlacklistEntry;
import com.ksa.financing.fraud.domain.port.out.CountryBlacklistRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CountryBlacklistRepositoryImpl implements CountryBlacklistRepository {

    @Override
    public CountryBlacklistEntry save(CountryBlacklistEntry entry) {
        return entry;
    }

    @Override
    public Optional<CountryBlacklistEntry> findActiveByCountryCode(UUID tenantId, String countryCode) {
        return Optional.empty();
    }

    @Override
    public List<CountryBlacklistEntry> findAllActive(UUID tenantId) {
        return List.of();
    }

    @Override
    public void deactivate(UUID tenantId, String countryCode) {
    }

    @Override
    public boolean isBlacklisted(UUID tenantId, String countryCode) {
        return false;
    }
}
