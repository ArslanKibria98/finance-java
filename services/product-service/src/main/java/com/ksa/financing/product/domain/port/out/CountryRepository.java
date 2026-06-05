package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.Country;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CountryRepository {

    List<Country> findAllByTenant(UUID tenantId);
    PageResponse<Country> findAllByTenant(UUID tenantId, PageQuery pageQuery);
    List<Country> findGccByTenant(UUID tenantId);
    PageResponse<Country> findGccByTenant(UUID tenantId, PageQuery pageQuery);
    List<Country> findArabLeagueByTenant(UUID tenantId);
    PageResponse<Country> findArabLeagueByTenant(UUID tenantId, PageQuery pageQuery);
    List<Country> findSanctionedByTenant(UUID tenantId);
    PageResponse<Country> findSanctionedByTenant(UUID tenantId, PageQuery pageQuery);
    List<Country> findByRegion(UUID tenantId, String region);
    PageResponse<Country> findByRegion(UUID tenantId, String region, PageQuery pageQuery);
    List<Country> findByRiskTier(UUID tenantId, String riskTier);
    PageResponse<Country> findByRiskTier(UUID tenantId, String riskTier, PageQuery pageQuery);
    Optional<Country> findById(UUID id);
    Optional<Country> findByCode(UUID tenantId, String code);
    Optional<Country> findBySlug(UUID tenantId, String slug);
    Country save(Country country);
    void delete(UUID id);
}
