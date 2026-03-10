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
    public Country getCountry(UUID tenantId, UUID id) {
        log.debug("Getting country id={} for tenant={}", id, tenantId);
        return countryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("Country", id.toString()));
    }

    @Override
    @Transactional
    public Country createCountry(UUID tenantId, String code, String nameEn, String nameAr,
                                  String dialCode, String currencyCode, boolean gcc, int sortOrder) {
        log.info("Creating country code={} for tenant={}", code, tenantId);

        countryRepository.findByCode(tenantId, code.toUpperCase()).ifPresent(existing -> {
            throw new BusinessException(ErrorCodes.Product.DUPLICATE_CODE,
                    "Country with code already exists: " + code, code);
        });

        var country = new Country();
        country.setId(UUID.randomUUID());
        country.setTenantId(tenantId);
        country.setCode(code.toUpperCase());
        country.setNameEn(nameEn);
        country.setNameAr(nameAr);
        country.setDialCode(dialCode);
        country.setCurrencyCode(currencyCode != null ? currencyCode.toUpperCase() : null);
        country.setGcc(gcc);
        country.setActive(true);
        country.setSortOrder(sortOrder);
        country.setCreatedAt(Instant.now());
        country.setUpdatedAt(Instant.now());

        return countryRepository.save(country);
    }

    @Override
    @Transactional
    public Country updateCountry(UUID tenantId, UUID id, String nameEn, String nameAr,
                                  String dialCode, String currencyCode, boolean gcc,
                                  int sortOrder, boolean active) {
        log.info("Updating country id={} for tenant={}", id, tenantId);

        var country = countryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.forEntity("Country", id.toString()));

        country.setNameEn(nameEn);
        country.setNameAr(nameAr);
        country.setDialCode(dialCode);
        country.setCurrencyCode(currencyCode != null ? currencyCode.toUpperCase() : null);
        country.setGcc(gcc);
        country.setSortOrder(sortOrder);
        country.setActive(active);
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
