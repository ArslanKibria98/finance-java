package com.ksa.financing.lending.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.LoanAggregate;
import com.ksa.financing.lending.domain.model.LoanId;
import com.ksa.financing.lending.domain.model.LoanStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository {

    LoanAggregate save(LoanAggregate aggregate);

    Optional<LoanAggregate> findById(UUID tenantId, LoanId id);

    Optional<LoanAggregate> findByLoanNumber(UUID tenantId, String loanNumber);

    PageResponse<LoanAggregate> findByCustomer(UUID tenantId, UUID customerId, PageQuery query);

    PageResponse<LoanAggregate> findAllByTenant(UUID tenantId, PageQuery query);

    List<LoanAggregate> findByStatus(UUID tenantId, LoanStatus status);

    Optional<LoanAggregate> findByApplicationId(UUID tenantId, UUID applicationId);

    String generateLoanNumber(UUID tenantId);
}
