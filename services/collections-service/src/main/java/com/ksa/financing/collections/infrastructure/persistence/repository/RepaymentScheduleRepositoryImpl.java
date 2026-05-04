package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.RepaymentScheduleId;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.RepaymentSchedulePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RepaymentScheduleRepositoryImpl implements RepaymentScheduleRepository {

    private final JpaRepaymentScheduleRepository jpaRepository;
    private final RepaymentSchedulePersistenceMapper mapper;

    @Override
    public RepaymentScheduleAggregate save(RepaymentScheduleAggregate schedule) {
        // Load the managed entity (with current @Version on schedule + installments)
        // and merge aggregate state into it. Rebuilding a detached graph with version=0
        // and calling save() caused StaleObjectStateException on cascade merge.
        var existing = jpaRepository.findById(schedule.getId().getValue()).orElse(null);
        if (existing == null) {
            var entity = mapper.toJpa(schedule);
            var saved = jpaRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.mergeInto(existing, schedule);
        var saved = jpaRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RepaymentScheduleAggregate> findById(UUID tenantId, RepaymentScheduleId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RepaymentScheduleAggregate> findActiveByLoanId(UUID tenantId, UUID loanId) {
        return jpaRepository.findActiveByTenantIdAndLoanId(tenantId, loanId)
                .map(mapper::toDomain);
    }

    @Override
    public List<RepaymentScheduleAggregate> findAllActive() {
        return jpaRepository.findAllActive().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByScheduleNumber(UUID tenantId, String scheduleNumber) {
        return jpaRepository.existsByTenantIdAndScheduleNumber(tenantId, scheduleNumber);
    }
}
