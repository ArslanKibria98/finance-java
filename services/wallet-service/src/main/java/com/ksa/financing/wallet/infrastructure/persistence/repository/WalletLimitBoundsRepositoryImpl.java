package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import com.ksa.financing.wallet.domain.port.out.WalletLimitBoundsRepository;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletLimitPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalletLimitBoundsRepositoryImpl implements WalletLimitBoundsRepository {

    private final JpaWalletLimitBoundsRepository jpa;
    private final WalletLimitPersistenceMapper mapper;

    @Override
    public WalletLimitBounds save(WalletLimitBounds bounds) {
        return mapper.toDomain(jpa.save(mapper.toEntity(bounds)));
    }

    @Override
    public Optional<WalletLimitBounds> findByTenantId(UUID tenantId) {
        return jpa.findByTenantId(tenantId).map(mapper::toDomain);
    }
}
