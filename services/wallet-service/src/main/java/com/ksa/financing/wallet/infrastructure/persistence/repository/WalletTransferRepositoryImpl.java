package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.out.WalletTransferRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletTransferJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletTransferPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WalletTransferRepositoryImpl implements WalletTransferRepository {

    private final JpaWalletTransferRepository jpaRepo;
    private final WalletTransferPersistenceMapper mapper;

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
}
