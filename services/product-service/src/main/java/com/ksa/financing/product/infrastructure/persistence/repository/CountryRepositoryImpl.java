package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.product.domain.model.Country;
import com.ksa.financing.product.domain.port.out.CountryRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.CountryJpaEntity;
import com.ksa.financing.product.infrastructure.persistence.mapper.CountryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CountryRepositoryImpl implements CountryRepository {

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "code", "nameEn", "nameAr", "region", "subRegion",
            "isGcc", "isArabLeague", "isOicMember", "isSanctioned",
            "riskTier", "isActive"
    );

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "code", "nameEn", "nameAr", "alpha3Code", "nationalityEn", "nationalityAr"
    );

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
    public PageResponse<Country> findAllByTenant(UUID tenantId, PageQuery pageQuery) {
        log.debug("Listing active countries with pagination for tenantId={}", tenantId);

        Specification<CountryJpaEntity> activeAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("isActive"), true)
                );

        Specification<CountryJpaEntity> dynamic = SpecificationBuilder.<CountryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CountryJpaEntity> page = jpaCountryRepository.findAll(
                activeAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<Country> content = page.getContent().stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
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
    public PageResponse<Country> findGccByTenant(UUID tenantId, PageQuery pageQuery) {
        log.debug("Listing GCC countries with pagination for tenantId={}", tenantId);

        Specification<CountryJpaEntity> gccActiveAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("isGcc"), true),
                        cb.equal(root.get("isActive"), true)
                );

        Specification<CountryJpaEntity> dynamic = SpecificationBuilder.<CountryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CountryJpaEntity> page = jpaCountryRepository.findAll(
                gccActiveAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<Country> content = page.getContent().stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
    }

    @Override
    public List<Country> findArabLeagueByTenant(UUID tenantId) {
        log.debug("Listing Arab League countries for tenantId={}", tenantId);
        return jpaCountryRepository.findAllByTenantIdAndIsArabLeagueTrueAndIsActiveTrue(tenantId)
                .stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Country> findArabLeagueByTenant(UUID tenantId, PageQuery pageQuery) {
        log.debug("Listing Arab League countries with pagination for tenantId={}", tenantId);

        Specification<CountryJpaEntity> arabLeagueActiveAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("isArabLeague"), true),
                        cb.equal(root.get("isActive"), true)
                );

        Specification<CountryJpaEntity> dynamic = SpecificationBuilder.<CountryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CountryJpaEntity> page = jpaCountryRepository.findAll(
                arabLeagueActiveAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<Country> content = page.getContent().stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
    }

    @Override
    public List<Country> findSanctionedByTenant(UUID tenantId) {
        log.debug("Listing sanctioned countries for tenantId={}", tenantId);
        return jpaCountryRepository.findAllByTenantIdAndIsSanctionedTrue(tenantId)
                .stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Country> findSanctionedByTenant(UUID tenantId, PageQuery pageQuery) {
        log.debug("Listing sanctioned countries with pagination for tenantId={}", tenantId);

        Specification<CountryJpaEntity> sanctionedAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("isSanctioned"), true)
                );

        Specification<CountryJpaEntity> dynamic = SpecificationBuilder.<CountryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CountryJpaEntity> page = jpaCountryRepository.findAll(
                sanctionedAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<Country> content = page.getContent().stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
    }

    @Override
    public List<Country> findByRegion(UUID tenantId, String region) {
        log.debug("Listing countries by region={} for tenantId={}", region, tenantId);
        return jpaCountryRepository.findAllByTenantIdAndRegionAndIsActiveTrue(tenantId, region)
                .stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Country> findByRegion(UUID tenantId, String region, PageQuery pageQuery) {
        log.debug("Listing countries by region={} with pagination for tenantId={}", region, tenantId);

        Specification<CountryJpaEntity> regionActiveAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("region"), region),
                        cb.equal(root.get("isActive"), true)
                );

        Specification<CountryJpaEntity> dynamic = SpecificationBuilder.<CountryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CountryJpaEntity> page = jpaCountryRepository.findAll(
                regionActiveAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<Country> content = page.getContent().stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
    }

    @Override
    public List<Country> findByRiskTier(UUID tenantId, String riskTier) {
        log.debug("Listing countries by riskTier={} for tenantId={}", riskTier, tenantId);
        return jpaCountryRepository.findAllByTenantIdAndRiskTierAndIsActiveTrue(tenantId, riskTier)
                .stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Country> findByRiskTier(UUID tenantId, String riskTier, PageQuery pageQuery) {
        log.debug("Listing countries by riskTier={} with pagination for tenantId={}", riskTier, tenantId);

        Specification<CountryJpaEntity> riskTierActiveAndTenant = (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("riskTier"), riskTier),
                        cb.equal(root.get("isActive"), true)
                );

        Specification<CountryJpaEntity> dynamic = SpecificationBuilder.<CountryJpaEntity>builder()
                .filters(pageQuery.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(pageQuery.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<CountryJpaEntity> page = jpaCountryRepository.findAll(
                riskTierActiveAndTenant.and(dynamic),
                pageQuery.toPageable());

        List<Country> content = page.getContent().stream()
                .map(CountryPersistenceMapper::toDomain)
                .toList();

        return new PageResponse<>(content, PageMetadata.from(page));
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
    public Optional<Country> findBySlug(UUID tenantId, String slug) {
        log.debug("Finding country by slug={}, tenantId={}", slug, tenantId);
        return jpaCountryRepository.findByTenantIdAndSlug(tenantId, slug)
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
