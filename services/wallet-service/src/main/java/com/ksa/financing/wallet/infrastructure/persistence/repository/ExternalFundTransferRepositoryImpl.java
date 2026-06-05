package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.port.out.ExternalFundTransferRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.ExternalFundTransferJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.ExternalFundTransferPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExternalFundTransferRepositoryImpl implements ExternalFundTransferRepository {

    private final JpaExternalFundTransferRepository jpaRepo;
    private final ExternalFundTransferPersistenceMapper mapper;

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "transferNumber", "status", "direction", "counterpartyName",
            "counterpartyAccount", "purposeNote", "errorCode", "scotiaStatus"
    );

    @Override
    public ExternalFundTransfer save(ExternalFundTransfer transfer) {
        ExternalFundTransferJpaEntity entity = mapper.toEntity(transfer);
        ExternalFundTransferJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExternalFundTransfer> findById(UUID id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ExternalFundTransfer> findByMovementId(UUID movementId) {
        return jpaRepo.findByMovementId(movementId).map(mapper::toDomain);
    }

    @Override
    public Optional<ExternalFundTransfer> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepo.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<ExternalFundTransfer> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public PageResponse<ExternalFundTransfer> findAllByWallet(UUID walletId, PageQuery query) {
        Specification<ExternalFundTransferJpaEntity> walletSpec = (root, q, cb) ->
                cb.equal(root.get("walletId"), walletId);

        Specification<ExternalFundTransferJpaEntity> dynamic = SpecificationBuilder.<ExternalFundTransferJpaEntity>builder()
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<ExternalFundTransferJpaEntity> page = jpaRepo.findAll(
                walletSpec.and(dynamic), query.toPageable());

        return PageResponse.from(page, mapper::toDomain);
    }
}
