package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.Country;
import com.ksa.financing.product.domain.port.out.CountryRepository;
import com.ksa.financing.product.infrastructure.persistence.mapper.CountryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CountryRepositoryImpl implements CountryRepository {

    private final JpaCountryRepository jpaCountryRepository;

    @Override
    public List<Country> findAllByTenant(UUID tenantId) {
        log.debug("Listing active countries for tenantId={}", tenantId);
        return jpaCountryRepository.findAllByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Country> findGccByTenant(UUID tenantId) {
        log.debug("Listing GCC countries for tenantId={}", tenantId);
        return jpaCountryRepository.findAllByTenantIdAndIsGccTrueAndIsActiveTrue(tenantId)
                .stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Country> findById(UUID id) {
        log.debug("Finding country by id={}", id);
        return jpaCountryRepository.findById(id)
                .map(CountryPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Country> findByCode(UUID tenantId, String code) {
        log.debug("Finding country by code={}, tenantId={}", code, tenantId);
        return jpaCountryRepository.findByTenantIdAndCode(tenantId, code)
                .map(CountryPersistenceMapper::toDomain);
    }

    @Override
    public Country save(Country country) {
        log.debug("Saving country: code={}", country.getCode());
        var entity = CountryPersistenceMapper.toEntity(country);
        var saved = jpaCountryRepository.save(entity);
        return CountryPersistenceMapper.toDomain(saved);
    }

    @Override
    public void delete(UUID id) {
        log.debug("Deleting country id={}", id);
        jpaCountryRepository.deleteById(id);
    }
}
