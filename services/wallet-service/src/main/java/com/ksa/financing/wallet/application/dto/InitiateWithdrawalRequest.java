package com.ksa.financing.wallet.application.dto;

import com.ksa.financing.wallet.domain.iso.ChargeBearerType1Code;
import com.ksa.financing.wallet.domain.iso.ExternalPurpose1Code;
import com.ksa.financing.wallet.domain.iso.ServiceLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiateWithdrawalRequest(
        @NotNull(message = "sourceWalletId is required") UUID sourceWalletId,
        UUID beneficiaryId,
        @Size(max = 34) String destinationIban,
        @Size(max = 200) String beneficiaryName,
        @Size(max = 20)  String destinationBankCode,
        @Size(max = 120) String destinationBankName,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be > 0") BigDecimal amount,
        String currency,
        String channel,
        @Size(max = 280) String purposeNote,
        ExternalPurpose1Code purposeCode,
        ChargeBearerType1Code chargeBearer,
        ServiceLevel serviceLevel
) {}
