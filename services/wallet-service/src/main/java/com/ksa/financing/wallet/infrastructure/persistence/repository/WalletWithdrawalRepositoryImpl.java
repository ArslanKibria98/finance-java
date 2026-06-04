package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.pagination.SpecificationBuilder;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
import com.ksa.financing.wallet.domain.port.out.WalletWithdrawalRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.WalletWithdrawalJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.WalletWithdrawalPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WalletWithdrawalRepositoryImpl implements WalletWithdrawalRepository {

    private final JpaWalletWithdrawalRepository jpaRepo;
    private final WalletWithdrawalPersistenceMapper mapper;

    private static final Set<String> SEARCHABLE_FIELDS = Set.of(
            "withdrawalNumber", "destinationIban", "destinationBankName", 
            "destinationCountry", "beneficiaryName", "channel", "status"
    );

    @Override
    public WalletWithdrawal save(WalletWithdrawal w) {
        log.debug("Saving withdrawal id={} status={}", w.getId(), w.getStatus());
        WalletWithdrawalJpaEntity entity = mapper.toEntity(w);
        WalletWithdrawalJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    /** Withdrawal statuses that do NOT consume the transaction limit (failed / cancelled / refunded). */
    private static final List<String> NON_COUNTING_STATUSES = List.of("FAILED", "CANCELLED", "COMPENSATED");

    @Override
    public BigDecimal sumWithdrawnSince(UUID tenantId, UUID walletId, Instant since) {
        OffsetDateTime sinceOdt = since.atOffset(ZoneOffset.UTC);
        BigDecimal sum = jpaRepo.sumWithdrawnSince(tenantId, walletId, NON_COUNTING_STATUSES, sinceOdt);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public Optional<WalletWithdrawal> findById(UUID id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletWithdrawal> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepo.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletWithdrawal> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpaRepo.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<WalletWithdrawal> findBySourceWallet(UUID walletId) {
        return jpaRepo.findBySourceWalletIdOrderByInitiatedAtDesc(walletId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public PageResponse<WalletWithdrawal> findBySourceWallet(UUID walletId, PageQuery query) {
        log.debug("Finding withdrawals for wallet={} page={} search={}", walletId, query.page(), query.search());

        Specification<WalletWithdrawalJpaEntity> walletSpec = (root, q, cb) ->
                cb.equal(root.get("sourceWalletId"), walletId);

        Specification<WalletWithdrawalJpaEntity> dynamic = SpecificationBuilder.<WalletWithdrawalJpaEntity>builder()
                .search(query.search())
                .searchableFields(SEARCHABLE_FIELDS)
                .build();

        Page<WalletWithdrawalJpaEntity> page = jpaRepo.findAll(
                walletSpec.and(dynamic),
                query.toPageable());

        return PageResponse.from(page, mapper::toDomain);
    }
}
