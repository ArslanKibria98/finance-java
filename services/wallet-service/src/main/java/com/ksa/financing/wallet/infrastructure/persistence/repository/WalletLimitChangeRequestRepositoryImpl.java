package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import com.ksa.financing.wallet.domain.port.out.WalletLimitChangeRequestRepository;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletLimitPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalletLimitChangeRequestRepositoryImpl implements WalletLimitChangeRequestRepository {

    private final JpaWalletLimitChangeRequestRepository jpa;
    private final WalletLimitPersistenceMapper mapper;

    @Override
    public WalletLimitChangeRequest save(WalletLimitChangeRequest request) {
        return mapper.toDomain(jpa.save(mapper.toEntity(request)));
    }

    @Override
    public Optional<WalletLimitChangeRequest> findById(UUID id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WalletLimitChangeRequest> findByTenantAndStatus(UUID tenantId, LimitRequestStatus status) {
        return jpa.findByTenantIdAndStatusOrderByRequestedAtDesc(tenantId, status.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<WalletLimitChangeRequest> findByTenant(UUID tenantId) {
        return jpa.findByTenantIdOrderByRequestedAtDesc(tenantId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<WalletLimitChangeRequest> findByWallet(UUID walletId) {
        return jpa.findByWalletIdOrderByRequestedAtDesc(walletId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByWalletAndStatus(UUID walletId, LimitRequestStatus status) {
        return jpa.existsByWalletIdAndStatus(walletId, status.name());
    }
}
