package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.Country;
import java.util.List;
import java.util.UUID;

public interface ManageCountryUseCase {

    List<Country> listCountries(UUID tenantId);
    List<Country> listGccCountries(UUID tenantId);
    List<Country> listArabLeagueCountries(UUID tenantId);
    List<Country> listSanctionedCountries(UUID tenantId);
    List<Country> listByRegion(UUID tenantId, String region);
    List<Country> listByRiskTier(UUID tenantId, String riskTier);
    Country getCountry(UUID tenantId, UUID id);
    Country getCountryBySlug(UUID tenantId, String slug);
    Country createCountry(UUID tenantId, Country country);
    Country updateCountry(UUID tenantId, UUID id, Country country);
    void deleteCountry(UUID tenantId, UUID id);
}
