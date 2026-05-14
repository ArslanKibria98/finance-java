package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.lending.domain.model.LoanAggregate;
import com.ksa.financing.lending.domain.model.LoanId;
import com.ksa.financing.lending.domain.model.LoanStatus;
import com.ksa.financing.lending.domain.port.out.LoanRepository;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.mapper.LoanPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LoanRepositoryImpl implements LoanRepository {

    private final JpaLoanRepository jpaRepository;
    private final LoanPersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "status", "customerId", "productId", "productCode", "loanNumber"
    );

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "loanNumber", "productCode", "shariaStructure", "status"
    );

    @Override
    @Transactional
    public LoanAggregate save(LoanAggregate aggregate) {
        log.debug("Saving loan: {}", aggregate.getLoanNumber());
        var entity = mapper.toEntity(aggregate);
        entity = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanAggregate> findById(UUID tenantId, LoanId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanAggregate> findByLoanNumber(UUID tenantId, String loanNumber) {
        return jpaRepository.findByTenantIdAndLoanNumber(tenantId, loanNumber)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanAggregate> findByCustomer(UUID tenantId, UUID customerId, PageQuery query) {
        log.debug("Listing loans page={} size={} for customerId={}",
                query.page(), query.size(), customerId);

        Specification<LoanJpaEntity> customerSpec = (root, q, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("customerId"), customerId)
                );

        Specification<LoanJpaEntity> dynamic = SpecificationBuilder.<LoanJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<LoanJpaEntity> page = jpaRepository.findAll(
                customerSpec.and(dynamic),
                query.toPageable());

        List<LoanAggregate> aggregates = page.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResponse<>(aggregates, PageMetadata.from(page));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanAggregate> findAllByTenant(UUID tenantId, PageQuery query) {
        log.debug("Listing loans page={} size={} for tenantId={}",
                query.page(), query.size(), tenantId);

        Specification<LoanJpaEntity> tenantSpec = (root, q, cb) ->
                cb.equal(root.get("tenantId"), tenantId);

        Specification<LoanJpaEntity> dynamic = SpecificationBuilder.<LoanJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<LoanJpaEntity> page = jpaRepository.findAll(
                tenantSpec.and(dynamic),
                query.toPageable());

        List<LoanAggregate> aggregates = page.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResponse<>(aggregates, PageMetadata.from(page));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanAggregate> findByStatus(UUID tenantId, LoanStatus status) {
        return jpaRepository.findByTenantIdAndStatus(tenantId, status.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanAggregate> findByApplicationId(UUID tenantId, UUID applicationId) {
        return jpaRepository.findByTenantIdAndApplicationId(tenantId, applicationId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateLoanNumber(UUID tenantId) {
        int seq = jpaRepository.getNextLoanSequence(tenantId);
        return String.format("LOAN-%08d", seq);
    }
}
