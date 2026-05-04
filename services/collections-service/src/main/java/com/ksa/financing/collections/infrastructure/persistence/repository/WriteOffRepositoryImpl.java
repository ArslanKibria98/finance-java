package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.domain.model.WriteOffId;
import com.ksa.financing.collections.domain.model.WriteOffRecord;
import com.ksa.financing.collections.domain.port.out.WriteOffRepository;
import com.ksa.financing.collections.infrastructure.persistence.entity.WriteOffRecordJpaEntity;
import com.ksa.financing.collections.infrastructure.persistence.mapper.WriteOffRecordPersistenceMapper;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WriteOffRepositoryImpl implements WriteOffRepository {

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
            "loanId", "scheduleId", "installmentId", "delinquencyRuleId",
            "triggerType", "status", "writeOffDate", "initiatedBy",
            "reversedBy", "createdAt", "updatedAt"
    );

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "reason", "approvalReference", "reversalReason"
    );

    private final JpaWriteOffRecordRepository jpaRepository;
    private final WriteOffRecordPersistenceMapper mapper;

    @Override
    public WriteOffRecord save(WriteOffRecord record) {
        var saved = jpaRepository.save(mapper.toJpa(record));
        return mapper.toDomain(saved);
    }

    @Override
    public List<WriteOffRecord> saveAll(List<WriteOffRecord> records) {
        List<WriteOffRecord> out = new ArrayList<>(records.size());
        for (var r : records) {
            out.add(save(r));
        }
        return out;
    }

    @Override
    public Optional<WriteOffRecord> findById(UUID tenantId, WriteOffId id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<WriteOffRecord> findByLoanId(UUID tenantId, UUID loanId) {
        return jpaRepository.findByTenantIdAndLoanIdOrderByCreatedAtDesc(tenantId, loanId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<WriteOffRecord> findByInstallmentId(UUID tenantId, UUID installmentId) {
        return jpaRepository.findByTenantIdAndInstallmentIdOrderByCreatedAtDesc(tenantId, installmentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<WriteOffRecord> findByDateRange(UUID tenantId, LocalDate fromDate, LocalDate toDate) {
        return jpaRepository.findByDateRange(tenantId, fromDate, toDate).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<WriteOffRecord> findAllByTenant(UUID tenantId, PageQuery query) {
        Specification<WriteOffRecordJpaEntity> tenantOnly = (root, q, cb) ->
                cb.equal(root.get("tenantId"), tenantId);

        Specification<WriteOffRecordJpaEntity> dynamic = SpecificationBuilder.<WriteOffRecordJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<WriteOffRecordJpaEntity> page = jpaRepository.findAll(
                tenantOnly.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }
}
