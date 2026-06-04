package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private static final Logger log = LoggerFactory.getLogger(WalletRepositoryImpl.class);

    private final JpaWalletRepository jpaWalletRepository;
    private final WalletPersistenceMapper mapper;

    @Override
    public Wallet save(Wallet wallet) {
        log.debug("Saving wallet for customer: {}", wallet.getCustomerId());
        WalletJpaEntity entity = mapper.toEntity(wallet);
        WalletJpaEntity saved = jpaWalletRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        log.debug("Finding wallet by id: {}", id);
        return jpaWalletRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByCustomerId(UUID tenantId, UUID customerId) {
        log.debug("Finding wallet by customerId: {} tenantId: {}", customerId, tenantId);
        return jpaWalletRepository.findByCustomerIdAndTenantId(customerId, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByWalletNumber(String walletNumber) {
        log.debug("Finding wallet by walletNumber: {}", walletNumber);
        return jpaWalletRepository.findByWalletNumber(walletNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByIban(UUID tenantId, String iban) {
        log.debug("Finding wallet by iban tenantId: {}", tenantId);
        return jpaWalletRepository.findByIbanAndTenantId(iban, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findByAccountNumber(UUID tenantId, String accountNumber) {
        log.debug("Finding wallet by accountNumber tenantId: {}", tenantId);
        return jpaWalletRepository.findByAccountNumberAndTenantId(accountNumber, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public long nextAccountNumberSequence() {
        return jpaWalletRepository.nextAccountNumberSequence();
    }

    @Override
    public boolean existsByCustomerId(UUID tenantId, UUID customerId) {
        log.debug("Checking wallet existence for customerId: {} tenantId: {}", customerId, tenantId);
        return jpaWalletRepository.existsByCustomerIdAndTenantId(customerId, tenantId);
    }
}
