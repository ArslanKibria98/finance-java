package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.IbanBeneficiary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IbanBeneficiaryRepository {

    IbanBeneficiary save(IbanBeneficiary beneficiary);

    Optional<IbanBeneficiary> findById(UUID id);

    Optional<IbanBeneficiary> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<IbanBeneficiary> findByCustomerAndIban(UUID tenantId, UUID customerId, String iban);

    List<IbanBeneficiary> findActiveByCustomer(UUID tenantId, UUID customerId);
}
