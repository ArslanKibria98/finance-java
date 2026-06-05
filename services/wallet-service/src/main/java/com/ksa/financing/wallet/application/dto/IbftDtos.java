package com.ksa.financing.wallet.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class IbftDtos {

    private IbftDtos() {}

    /**
     * Either {@code beneficiaryId} (saved payee) OR the one-time inline payee fields
     * (institutionNumber + accountNumber + beneficiaryName). transit is NOT supplied —
     * it is derived from the first 5 digits of {@code accountNumber}.
     * A stray {@code transit} sent by older clients is ignored (not rejected).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitiateIbftRequest(
            UUID beneficiaryId,
            String institutionNumber,
            String accountNumber,
            String beneficiaryName,
            String bankName,
            @NotNull @Positive BigDecimal amount,
            String currency,
            String purposeNote
    ) {}

    public record IbftResponse(
            UUID id,
            String ibftNumber,
            String status,
            UUID walletId,
            UUID beneficiaryId,
            String creditorName,
            String creditorAccount,
            BigDecimal amount,
            String currency,
            String purposeNote,
            String scotiaSubmissionId,
            String scotiaStatus,
            UUID holdMovementId,
            UUID debitMovementId,
            UUID releaseMovementId,
            UUID ledgerEntryId,
            String errorCode,
            String errorMessage,
            Instant initiatedAt,
            Instant submittedAt,
            Instant settledAt,
            Instant failedAt
    ) {
        public static IbftResponse from(IbftTransaction t) {
            return new IbftResponse(
                    t.getId(), t.getIbftNumber(), t.getStatus() != null ? t.getStatus().name() : null,
                    t.getWalletId(), t.getBeneficiaryId(), t.getCreditorName(), t.getCreditorAccount(),
                    t.getAmount(), t.getCurrency(), t.getPurposeNote(),
                    t.getScotiaSubmissionId(), t.getScotiaStatus(),
                    t.getHoldMovementId(), t.getDebitMovementId(), t.getReleaseMovementId(), t.getLedgerEntryId(),
                    t.getErrorCode(), t.getErrorMessage(),
                    t.getInitiatedAt(), t.getSubmittedAt(), t.getSettledAt(), t.getFailedAt());
        }
    }
}
