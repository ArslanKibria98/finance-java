package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.PenaltyWaiver;
import com.ksa.financing.collections.domain.model.PenaltyWaiverId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenaltyWaiverRepository {

    PenaltyWaiver save(PenaltyWaiver waiver);

    Optional<PenaltyWaiver> findById(UUID tenantId, PenaltyWaiverId id);

    List<PenaltyWaiver> findByLoanId(UUID tenantId, UUID loanId);

    List<PenaltyWaiver> findByInstallmentId(UUID tenantId, UUID installmentId);

    List<PenaltyWaiver> findByDateRange(UUID tenantId, LocalDate fromDate, LocalDate toDate);

    List<PenaltyWaiver> findAll(UUID tenantId, int page, int size);
}
