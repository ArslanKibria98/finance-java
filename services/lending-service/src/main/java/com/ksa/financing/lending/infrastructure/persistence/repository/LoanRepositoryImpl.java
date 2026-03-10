package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.domain.model.LoanAggregate;
import com.ksa.financing.lending.domain.model.LoanId;
import com.ksa.financing.lending.domain.model.LoanStatus;
import com.ksa.financing.lending.domain.port.out.LoanRepository;
import com.ksa.financing.lending.infrastructure.persistence.mapper.LoanPersistenceMapper;
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
public class LoanRepositoryImpl implements LoanRepository {

    private final JpaLoanRepository jpaRepository;
    private final LoanPersistenceMapper mapper;

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
    public List<LoanAggregate> findByCustomer(UUID tenantId, UUID customerId) {
        return jpaRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanAggregate> findByStatus(UUID tenantId, LoanStatus status) {
        return jpaRepository.findByTenantIdAndStatus(tenantId, status.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String generateLoanNumber(UUID tenantId) {
        int seq = jpaRepository.getNextLoanSequence(tenantId);
        return String.format("LN-%08d", seq);
    }
}
