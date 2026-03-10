package com.ksa.financing.globalprofile.infrastructure.persistence.repository;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.port.out.GlobalCustomerRepository;
import com.ksa.financing.globalprofile.infrastructure.persistence.entity.GlobalCustomerJpaEntity;
import com.ksa.financing.globalprofile.infrastructure.persistence.mapper.GlobalProfilePersistenceMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link GlobalCustomerRepository}.
 * <p>
 * IMPORTANT: The DB has triggers that auto-compute:
 * <ul>
 *   <li>{@code updated_at} and {@code version} on UPDATE (trigger_global_customers_updated)</li>
 *   <li>{@code global_kyc_status} from regional profiles (trigger_compute_global_kyc)</li>
 * </ul>
 * After save, we flush and refresh to pick up trigger-modified values.
 */
@Component
@RequiredArgsConstructor
public class GlobalCustomerRepositoryImpl implements GlobalCustomerRepository {

    private final JpaGlobalCustomerRepository jpaRepository;
    private final GlobalProfilePersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public GlobalCustomer save(GlobalCustomer globalCustomer) {
        GlobalCustomerJpaEntity entity = mapper.toEntity(globalCustomer);
        GlobalCustomerJpaEntity saved = jpaRepository.save(entity);

        // Flush to trigger DB-level triggers, then refresh to pick up computed values
        entityManager.flush();
        entityManager.refresh(saved);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<GlobalCustomer> findByGlobalUid(UUID globalUid) {
        return jpaRepository.findByGlobalUid(globalUid)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GlobalCustomer> findByEmailHash(String emailHash) {
        return jpaRepository.findByGlobalEmailHash(emailHash)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GlobalCustomer> findByMobileHash(String mobileHash) {
        return jpaRepository.findByGlobalMobileHash(mobileHash)
                .map(mapper::toDomain);
    }
}
