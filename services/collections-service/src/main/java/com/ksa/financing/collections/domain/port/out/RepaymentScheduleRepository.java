package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.RepaymentScheduleId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepaymentScheduleRepository {

    RepaymentScheduleAggregate save(RepaymentScheduleAggregate schedule);

    Optional<RepaymentScheduleAggregate> findById(UUID tenantId, RepaymentScheduleId id);

    Optional<RepaymentScheduleAggregate> findActiveByLoanId(UUID tenantId, UUID loanId);

    List<RepaymentScheduleAggregate> findAllActive();

    boolean existsByScheduleNumber(UUID tenantId, String scheduleNumber);
}
