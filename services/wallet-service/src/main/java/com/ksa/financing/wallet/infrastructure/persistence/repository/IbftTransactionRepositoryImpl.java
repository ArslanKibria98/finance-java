package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.domain.port.out.IbftTransactionRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftTransactionJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.IbftTransactionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IbftTransactionRepositoryImpl implements IbftTransactionRepository {

    private final JpaIbftTransactionRepository jpaRepo;
    private final IbftTransactionPersistenceMapper mapper;

    private static final Set<String> SEARCHABLE = Set.of(
            "ibftNumber", "status", "creditorName", "creditorAccount", "purposeNote", "scotiaStatus", "errorCode");

    @Override
    public IbftTransaction save(IbftTransaction tx) {
        return mapper.toDomain(jpaRepo.save(mapper.toEntity(tx)));
    }

    @Override
    public Optional<IbftTransaction> findById(UUID id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<IbftTransaction> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepo.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<IbftTransaction> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public PageResponse<IbftTransaction> findAllByWallet(UUID walletId, PageQuery query) {
        Specification<IbftTransactionJpaEntity> walletSpec = (root, q, cb) -> cb.equal(root.get("walletId"), walletId);
        Specification<IbftTransactionJpaEntity> dynamic = SpecificationBuilder.<IbftTransactionJpaEntity>builder()
                .search(query.search()).searchableFields(SEARCHABLE).build();
        Page<IbftTransactionJpaEntity> page = jpaRepo.findAll(walletSpec.and(dynamic), query.toPageable());
        return PageResponse.from(page, mapper::toDomain);
    }

    @Override
    public List<IbftTransaction> findReconcilable(int limit) {
        return jpaRepo.findReconcilable(PageRequest.of(0, limit)).stream().map(mapper::toDomain).toList();
    }
}
