package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.out.WalletTransferRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletTransferJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletTransferPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WalletTransferRepositoryImpl implements WalletTransferRepository {

    private final JpaWalletTransferRepository jpaRepo;
    private final WalletTransferPersistenceMapper mapper;

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "transferNumber", "status", "channel", "purposeNote", "errorCode", "errorMessage"
    );

    @Override
    public List<UUID> findRecentRecipientWalletIds(UUID sourceWalletId, int limit) {
        log.debug("Finding recent recipient wallet IDs for sourceWallet={} limit={}", sourceWalletId, limit);
        return jpaRepo.findRecentRecipientWalletIds(sourceWalletId, PageRequest.of(0, limit));
    }

    @Override
    public Optional<WalletTransfer> findMostRecentTransferToRecipient(UUID sourceWalletId, UUID destinationWalletId) {
        return jpaRepo
                .findFirstBySourceWalletIdAndDestinationWalletIdOrderByInitiatedAtDesc(sourceWalletId, destinationWalletId)
                .map(mapper::toDomain);
    }

    @Override
    public WalletTransfer save(WalletTransfer transfer) {
        log.debug("Saving wallet transfer id={} status={}", transfer.getId(), transfer.getStatus());
        WalletTransferJpaEntity entity = mapper.toEntity(transfer);
        WalletTransferJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WalletTransfer> findById(UUID id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletTransfer> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepo.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletTransfer> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<WalletTransfer> findBySourceWallet(UUID walletId) {
        return jpaRepo.findBySourceWalletIdOrderByInitiatedAtDesc(walletId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<WalletTransfer> findByDestinationWallet(UUID walletId) {
        return jpaRepo.findByDestinationWalletIdOrderByInitiatedAtDesc(walletId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResponse<WalletTransfer> findAllByWallet(UUID walletId, PageQuery query) {
        log.debug("Finding all transfers (sent/received) for wallet={} page={} search={}", 
                walletId, query.page(), query.search());

        Specification<WalletTransferJpaEntity> walletSpec = (root, q, cb) ->
                cb.or(
                        cb.equal(root.get("sourceWalletId"), walletId),
                        cb.equal(root.get("destinationWalletId"), walletId)
                );

        Specification<WalletTransferJpaEntity> dynamic = SpecificationBuilder.<WalletTransferJpaEntity>builder()
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<WalletTransferJpaEntity> page = jpaRepo.findAll(
                walletSpec.and(dynamic),
                query.toPageable());

        return PageResponse.from(page, mapper::toDomain);
    }
}
