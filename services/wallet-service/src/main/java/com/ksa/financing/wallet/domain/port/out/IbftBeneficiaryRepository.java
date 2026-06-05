package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.IbftBeneficiary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IbftBeneficiaryRepository {
    IbftBeneficiary save(IbftBeneficiary beneficiary);
    Optional<IbftBeneficiary> findById(UUID id);
    Optional<IbftBeneficiary> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<IbftBeneficiary> findExisting(UUID tenantId, UUID customerId,
                                           String institutionNumber, String transit, String accountNumber);
    List<IbftBeneficiary> findByCustomer(UUID tenantId, UUID customerId);
}
