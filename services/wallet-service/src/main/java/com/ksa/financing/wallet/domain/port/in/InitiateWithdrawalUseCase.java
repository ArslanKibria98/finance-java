package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.iso.ChargeBearerType1Code;
import com.ksa.financing.wallet.domain.iso.ExternalPurpose1Code;
import com.ksa.financing.wallet.domain.iso.ServiceLevel;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Initiate an outbound wallet withdrawal (cash-out to bank IBAN).
 */
public interface InitiateWithdrawalUseCase {

    WalletWithdrawal initiate(InitiateWithdrawalCommand command);

    record InitiateWithdrawalCommand(
            UUID tenantId,
            UUID sourceWalletId,
            // Destination — either a saved beneficiaryId, or ad-hoc IBAN fields
            UUID beneficiaryId,
            String destinationIban,
            String beneficiaryName,
            String destinationBankCode,
            String destinationBankName,
            BigDecimal amount,
            String currency,
            String channel,
            String purposeNote,
            ExternalPurpose1Code purposeCode,
            ChargeBearerType1Code chargeBearer,
            ServiceLevel serviceLevel,
            String idempotencyKey,
            UUID initiatorUserId,
            String initiatorIp,
            String initiatorDeviceId
    ) {}
}
