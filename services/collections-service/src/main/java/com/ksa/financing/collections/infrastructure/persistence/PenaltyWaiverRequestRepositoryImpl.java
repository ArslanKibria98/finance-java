package com.ksa.financing.collections.infrastructure.persistence;

import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRequestRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.PenaltyWaiverRequestPersistenceMapper;
import com.ksa.financing.collections.infrastructure.persistence.repository.PenaltyWaiverRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PenaltyWaiverRequestRepositoryImpl implements PenaltyWaiverRequestRepository {

    private final PenaltyWaiverRequestJpaRepository jpaRepository;
    private final PenaltyWaiverRequestPersistenceMapper mapper;

    @Override
    public PenaltyWaiverRequest save(PenaltyWaiverRequest request) {
        var entity = mapper.toEntity(request);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PenaltyWaiverRequest> findById(UUID tenantId, UUID requestId) {
        return jpaRepository.findById(requestId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .map(mapper::toDomain);
    }

    @Override
    public List<PenaltyWaiverRequest> findByLoanId(UUID tenantId, UUID loanId) {
        return jpaRepository.findAllByTenantIdAndLoanId(tenantId, loanId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiverRequest> findByApplicationId(UUID tenantId, UUID applicationId) {
        return jpaRepository.findAllByTenantIdAndApplicationId(tenantId, applicationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiverRequest> findByInvoiceId(UUID tenantId, String invoiceId) {
        return jpaRepository.findAllByTenantIdAndInvoiceId(tenantId, invoiceId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiverRequest> findByRequestedBy(UUID tenantId, UUID customerId) {
        return jpaRepository.findAllByTenantIdAndRequestedBy(tenantId, customerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiverRequest> findByStatus(UUID tenantId, PenaltyWaiverRequest.WaiverRequestStatus status) {
        return jpaRepository.findAllByTenantIdAndStatus(tenantId, status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countApprovedByLoanId(UUID tenantId, UUID loanId) {
        return jpaRepository.countByTenantIdAndLoanIdAndStatus(tenantId, loanId,
                PenaltyWaiverRequest.WaiverRequestStatus.APPROVED.name());
    }

    @Override
    public PagedResult findPaged(UUID tenantId,
                                 PenaltyWaiverRequest.WaiverRequestStatus status,
                                 int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "requestedAt"));
        var pageResult = (status == null)
                ? jpaRepository.findAllByTenantId(tenantId, pageable)
                : jpaRepository.findAllByTenantIdAndStatus(tenantId, status.name(), pageable);
        var content = pageResult.getContent().stream().map(mapper::toDomain).toList();
        return new PagedResult(content, pageResult.getTotalElements(), page, size);
    }
}
