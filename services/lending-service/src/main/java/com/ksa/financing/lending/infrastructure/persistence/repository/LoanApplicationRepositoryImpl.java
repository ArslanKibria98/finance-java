package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanApplicationJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.mapper.LoanApplicationPersistenceMapper;
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
public class LoanApplicationRepositoryImpl implements LoanApplicationRepository {

    private final JpaLoanApplicationRepository jpaRepository;
    private final LoanApplicationPersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "status", "customerId", "productId", "productCode", "nationalId", "applicationNumber"
    );

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "applicationNumber", "nationalId", "disbursementIban", "productCode", "status"
    );

    @Override
    @Transactional
    public LoanApplicationAggregate save(LoanApplicationAggregate aggregate) {
        log.debug("Saving loan application: {}", aggregate.getApplicationNumber());
        var entity = mapper.toEntity(aggregate);
        entity = jpaRepository.saveAndFlush(entity);
        return mapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanApplicationAggregate> findById(UUID tenantId, LoanApplicationId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanApplicationAggregate> findByApplicationNumber(UUID tenantId, String applicationNumber) {
        return jpaRepository.findByTenantIdAndApplicationNumber(tenantId, applicationNumber)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanApplicationAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoanApplicationAggregate> findByWorkflowId(UUID tenantId, String workflowId) {
        return jpaRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanApplicationAggregate> findAllByTenant(UUID tenantId, PageQuery query) {
        log.debug("Listing applications page={} size={} for tenantId={}",
                query.page(), query.size(), tenantId);

        Specification<LoanApplicationJpaEntity> tenantSpec = (root, q, cb) ->
                cb.equal(root.get("tenantId"), tenantId);

        Specification<LoanApplicationJpaEntity> dynamic = SpecificationBuilder.<LoanApplicationJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<LoanApplicationJpaEntity> page = jpaRepository.findAll(
                tenantSpec.and(dynamic),
                query.toPageable());

        List<LoanApplicationAggregate> aggregates = page.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResponse<>(aggregates, PageMetadata.from(page));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanApplicationAggregate> findByCustomer(UUID tenantId, UUID customerId, PageQuery query) {
        log.debug("Listing applications page={} size={} for customerId={}",
                query.page(), query.size(), customerId);

        Specification<LoanApplicationJpaEntity> customerSpec = (root, q, cb) ->
                cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("customerId"), customerId)
                );

        Specification<LoanApplicationJpaEntity> dynamic = SpecificationBuilder.<LoanApplicationJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<LoanApplicationJpaEntity> page = jpaRepository.findAll(
                customerSpec.and(dynamic),
                query.toPageable());

        List<LoanApplicationAggregate> aggregates = page.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResponse<>(aggregates, PageMetadata.from(page));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationAggregate> findByStatus(UUID tenantId, ApplicationStatus status) {
        return jpaRepository.findByTenantIdAndStatus(tenantId, status.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByApplicationNumber(UUID tenantId, String applicationNumber) {
        return jpaRepository.existsByTenantIdAndApplicationNumber(tenantId, applicationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateApplicationNumber(UUID tenantId) {
        int seq = jpaRepository.getNextApplicationSequence(tenantId);
        return String.format("APP-%08d", seq);
    }
}
