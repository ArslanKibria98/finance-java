package com.ksa.financing.fraud.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LendingPort {

    Optional<DisbursementInfo> fetchDisbursement(UUID tenantId, String loanApplicationId);

    Optional<ScheduledInstallment> fetchScheduledInstallment(UUID tenantId, String loanId, int installmentNo);

    record DisbursementInfo(
        String loanId,
        String loanApplicationId,
        BigDecimal approvedAmount,
        BigDecimal actualDisbursedAmount,
        String disbursementIban,
        String ibanVerificationStatus,
        String ibanHolderName,
        LocalDateTime scheduledAt,
        LocalDateTime disbursedAt
    ) {}

    record ScheduledInstallment(
        String loanId,
        int installmentNo,
        BigDecimal scheduledAmount,
        LocalDateTime dueDate
    ) {}
}
