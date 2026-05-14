package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.IbanBeneficiary;
import com.ksa.financing.wallet.domain.port.out.IbanBeneficiaryRepository;
import com.ksa.financing.wallet.infrastructure.persistence.entity.IbanBeneficiaryJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.IbanBeneficiaryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IbanBeneficiaryRepositoryImpl implements IbanBeneficiaryRepository {

    private final JpaIbanBeneficiaryRepository jpaRepo;
    private final IbanBeneficiaryPersistenceMapper mapper;

    @Override
    public IbanBeneficiary save(IbanBeneficiary b) {
        log.debug("Saving beneficiary id={} customer={}", b.getId(), b.getCustomerId());
        IbanBeneficiaryJpaEntity entity = mapper.toEntity(b);
        IbanBeneficiaryJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<IbanBeneficiary> findById(UUID id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<IbanBeneficiary> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepo.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<IbanBeneficiary> findByCustomerAndIban(UUID tenantId, UUID customerId, String iban) {
        return jpaRepo.findByTenantIdAndCustomerIdAndIban(tenantId, customerId, iban).map(mapper::toDomain);
    }

    @Override
    public List<IbanBeneficiary> findActiveByCustomer(UUID tenantId, UUID customerId) {
        return jpaRepo.findByTenantIdAndCustomerIdAndActiveTrueOrderByCreatedAtDesc(tenantId, customerId)
                .stream().map(mapper::toDomain).toList();
    }
}
