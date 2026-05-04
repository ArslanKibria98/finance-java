package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.PenaltyWaiver;
import com.ksa.financing.collections.domain.model.PenaltyWaiverId;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRepository;
import com.ksa.financing.collections.infrastructure.persistence.mapper.PenaltyWaiverPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PenaltyWaiverRepositoryImpl implements PenaltyWaiverRepository {

    private final JpaPenaltyWaiverRepository jpaRepository;
    private final PenaltyWaiverPersistenceMapper mapper;

    @Override
    public PenaltyWaiver save(PenaltyWaiver waiver) {
        var saved = jpaRepository.save(mapper.toJpa(waiver));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PenaltyWaiver> findById(UUID tenantId, PenaltyWaiverId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<PenaltyWaiver> findByLoanId(UUID tenantId, UUID loanId) {
        return jpaRepository.findByTenantIdAndLoanIdOrderByWaivedAtDesc(tenantId, loanId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiver> findByInstallmentId(UUID tenantId, UUID installmentId) {
        return jpaRepository.findByTenantIdAndInstallmentIdOrderByWaivedAtDesc(tenantId, installmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiver> findByDateRange(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        var from = fromDate.atStartOfDay();
        var to = toDate.plusDays(1).atStartOfDay();
        return jpaRepository.findByDateRange(tenantId, from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<PenaltyWaiver> findAll(UUID tenantId, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)),
                Sort.by(Sort.Direction.DESC, "waivedAt"));
        return jpaRepository.findByTenantIdOrderByWaivedAtDesc(tenantId, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
