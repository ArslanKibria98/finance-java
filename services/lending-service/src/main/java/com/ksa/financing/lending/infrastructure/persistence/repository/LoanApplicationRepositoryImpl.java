package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import com.ksa.financing.lending.infrastructure.persistence.mapper.LoanApplicationPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LoanApplicationRepositoryImpl implements LoanApplicationRepository {

    private final JpaLoanApplicationRepository jpaRepository;
    private final LoanApplicationPersistenceMapper mapper;

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
    public List<LoanApplicationAggregate> findAllByTenant(UUID tenantId) {
        return jpaRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanApplicationAggregate> findByCustomer(UUID tenantId, UUID customerId) {
        return jpaRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .stream().map(mapper::toDomain).toList();
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
