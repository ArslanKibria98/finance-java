package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.domain.model.IbftBeneficiary;
import com.ksa.financing.wallet.domain.port.out.IbftBeneficiaryRepository;
import com.ksa.financing.wallet.infrastructure.persistence.mapper.IbftBeneficiaryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class IbftBeneficiaryRepositoryImpl implements IbftBeneficiaryRepository {

    private final JpaIbftBeneficiaryRepository jpaRepo;
    private final IbftBeneficiaryPersistenceMapper mapper;

    @Override
    public IbftBeneficiary save(IbftBeneficiary beneficiary) {
        return mapper.toDomain(jpaRepo.save(mapper.toEntity(beneficiary)));
    }

    @Override
    public Optional<IbftBeneficiary> findById(UUID id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<IbftBeneficiary> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpaRepo.findByIdAndTenantId(id, tenantId).map(mapper::toDomain);
    }

    @Override
    public Optional<IbftBeneficiary> findExisting(UUID tenantId, UUID customerId,
                                                  String institutionNumber, String transit, String accountNumber) {
        return jpaRepo.findByTenantIdAndCustomerIdAndInstitutionNumberAndTransitAndAccountNumber(
                tenantId, customerId, institutionNumber, transit, accountNumber).map(mapper::toDomain);
    }

    @Override
    public List<IbftBeneficiary> findByCustomer(UUID tenantId, UUID customerId) {
        return jpaRepo.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId)
                .stream().map(mapper::toDomain).toList();
    }
}
