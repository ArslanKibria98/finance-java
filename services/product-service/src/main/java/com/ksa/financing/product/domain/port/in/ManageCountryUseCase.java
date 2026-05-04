package com.ksa.financing.product.domain.port.in;

import com.ksa.financing.product.domain.model.Country;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import java.util.UUID;

public interface ManageCountryUseCase {

    PageResponse<Country> listCountries(UUID tenantId, PageQuery pageQuery);
    PageResponse<Country> listGccCountries(UUID tenantId, PageQuery pageQuery);
    PageResponse<Country> listArabLeagueCountries(UUID tenantId, PageQuery pageQuery);
    PageResponse<Country> listSanctionedCountries(UUID tenantId, PageQuery pageQuery);
    PageResponse<Country> listByRegion(UUID tenantId, String region, PageQuery pageQuery);
    PageResponse<Country> listByRiskTier(UUID tenantId, String riskTier, PageQuery pageQuery);
    Country getCountry(UUID tenantId, UUID id);
    Country getCountryBySlug(UUID tenantId, String slug);
    Country createCountry(UUID tenantId, Country country);
    Country updateCountry(UUID tenantId, UUID id, Country country);
    void deleteCountry(UUID tenantId, UUID id);
}
