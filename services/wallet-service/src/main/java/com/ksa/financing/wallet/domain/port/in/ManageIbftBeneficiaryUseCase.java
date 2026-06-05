package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.IbftBeneficiary;

import java.util.List;
import java.util.UUID;

public interface ManageIbftBeneficiaryUseCase {

    IbftBeneficiary add(AddBeneficiaryCommand command);

    List<IbftBeneficiary> list(UUID tenantId, UUID customerId);

    IbftBeneficiary get(UUID tenantId, UUID beneficiaryId);

    IbftBeneficiary deactivate(UUID tenantId, UUID beneficiaryId);

    record AddBeneficiaryCommand(
            UUID tenantId,
            UUID customerId,
            UUID walletId,
            String nickname,
            String beneficiaryName,
            String institutionNumber,
            String accountNumber,        // transit derived from its first 5 digits
            String bankName,
            String currency
    ) {}
}
