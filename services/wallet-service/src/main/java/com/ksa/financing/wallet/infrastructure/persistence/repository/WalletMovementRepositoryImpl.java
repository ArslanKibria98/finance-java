package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.WalletMovement;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletMovementJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalletMovementRepositoryImpl implements WalletMovementRepository {

    private static final Logger log = LoggerFactory.getLogger(WalletMovementRepositoryImpl.class);

    private final JpaWalletMovementRepository jpaWalletMovementRepository;
    private final WalletPersistenceMapper mapper;

    @Override
    public WalletMovement save(WalletMovement movement) {
        log.debug("Saving wallet movement: {} for wallet: {}", movement.getMovementNumber(), movement.getWalletId());
        WalletMovementJpaEntity entity = mapper.toEntity(movement);
        WalletMovementJpaEntity saved = jpaWalletMovementRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WalletMovement> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        log.debug("Finding movement by idempotency key: {} tenantId: {}", idempotencyKey, tenantId);
        return jpaWalletMovementRepository.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<WalletMovement> findByWalletId(UUID walletId) {
        log.debug("Finding movements for wallet: {}", walletId);
        return jpaWalletMovementRepository.findByWalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    /** Outgoing customer-initiated spend (transfers + withdrawals) since the given instant. */
    private static final List<String> SPEND_PURPOSES = List.of("TRANSFER_OUT", "WITHDRAWAL", "IBFT_HOLD");

    @Override
    public BigDecimal sumSpendSince(UUID tenantId, UUID walletId, Instant since) {
        OffsetDateTime sinceOdt = since.atOffset(ZoneOffset.UTC);
        BigDecimal sum = jpaWalletMovementRepository.sumSpendSince(tenantId, walletId, SPEND_PURPOSES, sinceOdt);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
