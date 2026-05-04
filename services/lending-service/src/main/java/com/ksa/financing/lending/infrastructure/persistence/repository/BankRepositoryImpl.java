package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.lending.domain.model.Bank;
import com.ksa.financing.lending.domain.port.out.BankRepository;
import com.ksa.financing.lending.infrastructure.persistence.entity.BankJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.mapper.BankPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BankRepositoryImpl implements BankRepository {

    private final JpaBankRepository jpaRepository;
    private final BankPersistenceMapper mapper;

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("active", "code");
    private static final Set<String> SEARCHABLE_FIELDS = Set.of("nameEn", "nameAr", "code");

    @Override
    public PageResponse<Bank> findAllActive(UUID tenantId, PageQuery query) {
        Specification<BankJpaEntity> tenantSpec = (root, q, cb) ->
                cb.and(
                    cb.equal(root.get("tenantId"), tenantId),
                    cb.equal(root.get("active"), true)
                );

        Specification<BankJpaEntity> dynamic = SpecificationBuilder.<BankJpaEntity>builder()
                .filters(query.filters())
                .allowedFilterFields(ALLOWED_FILTER_FIELDS)
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<BankJpaEntity> page = jpaRepository.findAll(tenantSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }
}
