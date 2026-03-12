package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.model.Country;
import com.ksa.financing.product.domain.port.in.ManageCountryUseCase;
import com.ksa.financing.product.domain.port.out.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageCountryUseCaseImpl implements ManageCountryUseCase {

    private final CountryRepository countryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Country> listCountries(UUID tenantId) {
        log.debug("Listing countries for tenant: {}", tenantId);
        return countryRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Country> listGccCountries(UUID tenantId) {
        log.debug("Listing GCC countries for tenant: {}", tenantId);
        return countryRepository.findGccByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Country> listArabLeagueCountries(UUID tenantId) {
        log.debug("Listing Arab League countries for tenant: {}", tenantId);
        return countryRepository.findArabLeagueByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Country> listSanctionedCountries(UUID tenantId) {
        log.debug("Listing sanctioned countries for tenant: {}", tenantId);
        return countryRepository.findSanctionedByTenant(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Country> listByRegion(UUID tenantId, String region) {
        log.debug("Listing countries for tenant: {} region: {}", tenantId, region);
        return countryRepository.findByRegion(tenantId, region);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Country> listByRiskTier(UUID tenantId, String riskTier) {
        log.debug("Listing countries for tenant: {} riskTier: {}", tenantId, riskTier);
        return countryRepository.findByRiskTier(tenantId, riskTier);
    }

    @Override
    @Transactional(readOnly = true)
    public Country getCountry(UUID tenantId, UUID id) {
        log.debug("Getting country id={} for tenant={}", id, tenantId);
        return countryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("Country", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Country getCountryBySlug(UUID tenantId, String slug) {
        log.debug("Getting country slug={} for tenant={}", slug, tenantId);
        return countryRepository.findBySlug(tenantId, slug)
                .orElseThrow(() -> NotFoundException.forEntity("Country", slug));
    }

    @Override
    @Transactional
    public Country createCountry(UUID tenantId, Country country) {
        log.info("Creating country code={} for tenant={}", country.getCode(), tenantId);

        countryRepository.findByCode(tenantId, country.getCode().toUpperCase()).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.Product.DUPLICATE_CODE,
                    "Country with code already exists: " + country.getCode(), country.getCode());
        });

        country.setId(UUID.randomUUID());
        country.setTenantId(tenantId);
        country.setCode(country.getCode().toUpperCase());
        if (country.getCurrencyCode() != null) {
            country.setCurrencyCode(country.getCurrencyCode().toUpperCase());
        }
        if (country.getAlpha3Code() != null) {
            country.setAlpha3Code(country.getAlpha3Code().toUpperCase());
        }
        if (country.getRiskTier() == null) {
            country.setRiskTier("STANDARD");
        }
        country.setActive(true);
        country.setCreatedAt(Instant.now());
        country.setUpdatedAt(Instant.now());

        return countryRepository.save(country);
    }

    @Override
    @Transactional
    public Country updateCountry(UUID tenantId, UUID id, Country updates) {
        log.info("Updating country id={} for tenant={}", id, tenantId);

        var country = countryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("Country", id.toString()));

        country.setAlpha3Code(updates.getAlpha3Code());
        country.setNumericCode(updates.getNumericCode());
        country.setSlug(updates.getSlug());
        country.setNameEn(updates.getNameEn());
        country.setNameAr(updates.getNameAr());
        country.setNationalityEn(updates.getNationalityEn());
        country.setNationalityAr(updates.getNationalityAr());
        country.setDialCode(updates.getDialCode());
        country.setCurrencyCode(updates.getCurrencyCode() != null ? updates.getCurrencyCode().toUpperCase() : null);
        country.setCurrencyNameEn(updates.getCurrencyNameEn());
        country.setCurrencyNameAr(updates.getCurrencyNameAr());
        country.setFlagEmoji(updates.getFlagEmoji());
        country.setCapitalEn(updates.getCapitalEn());
        country.setCapitalAr(updates.getCapitalAr());
        country.setRegion(updates.getRegion());
        country.setSubRegion(updates.getSubRegion());
        country.setGcc(updates.isGcc());
        country.setArabLeague(updates.isArabLeague());
        country.setOicMember(updates.isOicMember());
        country.setSanctioned(updates.isSanctioned());
        country.setRiskTier(updates.getRiskTier() != null ? updates.getRiskTier() : "STANDARD");
        country.setIbanRequired(updates.isIbanRequired());
        country.setIbanLength(updates.getIbanLength());
        country.setSortOrder(updates.getSortOrder());
        country.setActive(updates.isActive());
        country.setUpdatedAt(Instant.now());

        return countryRepository.save(country);
    }

    @Override
    @Transactional
    public void deleteCountry(UUID tenantId, UUID id) {
        log.info("Deleting country id={} for tenant={}", id, tenantId);
        countryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("Country", id.toString()));
        countryRepository.delete(id);
    }
}
