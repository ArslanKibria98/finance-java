package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.IbanBeneficiary;

import java.util.List;
import java.util.UUID;

/**
 * IBAN beneficiary management for wallet withdrawals.
 */
public interface ManageBeneficiaryUseCase {

    IbanBeneficiary add(AddBeneficiaryCommand command);

    List<IbanBeneficiary> listByCustomer(UUID tenantId, UUID customerId);

    IbanBeneficiary getById(UUID tenantId, UUID beneficiaryId);

    void deactivate(UUID tenantId, UUID beneficiaryId);

    IbanBeneficiary activate(UUID tenantId, UUID beneficiaryId);

    record AddBeneficiaryCommand(
            UUID tenantId,
            UUID customerId,
            UUID walletId,
            String nickname,
            String beneficiaryName,
            String iban,
            String bankCode,
            String bankName
    ) {}
}
