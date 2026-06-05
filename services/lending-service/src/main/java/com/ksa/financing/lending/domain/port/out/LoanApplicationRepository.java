package com.ksa.financing.lending.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanApplicationRepository {

    LoanApplicationAggregate save(LoanApplicationAggregate aggregate);

    Optional<LoanApplicationAggregate> findById(UUID tenantId, LoanApplicationId id);

    Optional<LoanApplicationAggregate> findByApplicationNumber(UUID tenantId, String applicationNumber);

    Optional<LoanApplicationAggregate> findByIdempotencyKey(UUID tenantId, String idempotencyKey);

    Optional<LoanApplicationAggregate> findByWorkflowId(UUID tenantId, String workflowId);

    PageResponse<LoanApplicationAggregate> findAllByTenant(UUID tenantId, PageQuery query);

    PageResponse<LoanApplicationAggregate> findByCustomer(UUID tenantId, UUID customerId, PageQuery query);

    List<LoanApplicationAggregate> findByStatus(UUID tenantId, ApplicationStatus status);

    boolean existsByApplicationNumber(UUID tenantId, String applicationNumber);

    String generateApplicationNumber(UUID tenantId);
}
