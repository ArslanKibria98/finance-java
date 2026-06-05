package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenaltyWaiverRequestRepository {
    PenaltyWaiverRequest save(PenaltyWaiverRequest request);
    Optional<PenaltyWaiverRequest> findById(UUID tenantId, UUID requestId);
    List<PenaltyWaiverRequest> findByLoanId(UUID tenantId, UUID loanId);
    List<PenaltyWaiverRequest> findByApplicationId(UUID tenantId, UUID applicationId);
    List<PenaltyWaiverRequest> findByInvoiceId(UUID tenantId, String invoiceId);
    List<PenaltyWaiverRequest> findByRequestedBy(UUID tenantId, UUID customerId);
    List<PenaltyWaiverRequest> findByStatus(UUID tenantId, PenaltyWaiverRequest.WaiverRequestStatus status);
    long countApprovedByLoanId(UUID tenantId, UUID loanId);

    PagedResult findPaged(UUID tenantId, PenaltyWaiverRequest.WaiverRequestStatus status, int page, int size);

    record PagedResult(List<PenaltyWaiverRequest> content, long totalElements, int page, int size) {
        public int totalPages() {
            return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        }
    }
}
