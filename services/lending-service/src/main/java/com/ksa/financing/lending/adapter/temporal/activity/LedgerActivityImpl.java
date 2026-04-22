package com.ksa.financing.lending.adapter.temporal.activity;

import com.ksa.financing.lending.infrastructure.client.LedgerServiceClient;
import com.ksa.islamic.orchestration.activity.lending.LedgerActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Temporal activity implementation for GL journal entries.
 *
 * Delegates to LedgerServiceClient which calls ledger-service REST API.
 * Ledger-service is the single GL bridge to Fineract — no direct Fineract
 * GL calls should come from lending-service.
 *
 * Temporal handles retries automatically via ActivityOptions configured
 * in LoanApplicationWorkflowImpl (ledgerOptions: 3 attempts, 30s timeout).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerActivityImpl implements LedgerActivity {

    private final LedgerServiceClient ledgerServiceClient;

    @Override
    public GlEntryResult postDisbursementGlEntry(DisbursementGlInput input) {
        log.info("Activity: Posting disbursement GL entry via ledger-service: loanId={} amount={} tenantId={}",
                input.loanId(), input.amount(), input.tenantId());

        UUID tenantId = UUID.fromString(input.tenantId());
        UUID loanId = UUID.fromString(input.loanId());
        UUID createdBy = input.createdBy() != null ? parseUuidSafely(input.createdBy()) : null;

        ledgerServiceClient.postDisbursementEntry(
                tenantId,
                loanId,
                input.loanNumber(),
                input.amount(),
                input.idempotencyKey(),
                createdBy
        );

        log.info("Disbursement GL entry posted successfully: loanId={}", input.loanId());
        return new GlEntryResult(null, "POSTED", true);
    }

    @Override
    public GlEntryResult postRepaymentGlEntry(RepaymentGlInput input) {
        log.info("Activity: Posting repayment GL entry via ledger-service: loanId={} amount={} tenantId={}",
                input.loanId(), input.amount(), input.tenantId());

        UUID tenantId = UUID.fromString(input.tenantId());
        UUID loanId = UUID.fromString(input.loanId());
        UUID createdBy = input.createdBy() != null ? parseUuidSafely(input.createdBy()) : null;

        ledgerServiceClient.postRepaymentEntry(
                tenantId,
                loanId,
                input.loanNumber(),
                input.amount(),
                input.idempotencyKey(),
                createdBy
        );

        log.info("Repayment GL entry posted successfully: loanId={}", input.loanId());
        return new GlEntryResult(null, "POSTED", true);
    }

    @Override
    public GlEntryResult postRestructuringWriteOffEntry(RestructuringGlInput input) {
        log.info("Activity: Posting restructuring write-off GL entry: loanId={} writeOff={} profitWaiver={}",
                input.loanId(), input.writeOffAmount(), input.profitWaiverAmount());

        UUID tenantId = UUID.fromString(input.tenantId());
        UUID loanId = UUID.fromString(input.loanId());
        UUID createdBy = input.createdBy() != null ? parseUuidSafely(input.createdBy()) : null;

        ledgerServiceClient.postRestructuringWriteOffEntry(
                tenantId,
                loanId,
                input.loanNumber(),
                input.writeOffAmount(),
                input.profitWaiverAmount(),
                input.idempotencyKey(),
                createdBy
        );

        log.info("Restructuring GL write-off entry posted: loanId={}", input.loanId());
        return new GlEntryResult(null, "POSTED", true);
    }

    @Override
    public GlEntryResult postSettlementGlEntry(SettlementGlInput input) {
        log.info("Activity: Posting settlement GL entry via ledger-service: loanId={} amount={} ibra={}",
                input.loanId(), input.settlementAmount(), input.ibraAmount());

        UUID tenantId = UUID.fromString(input.tenantId());
        UUID loanId = UUID.fromString(input.loanId());
        UUID createdBy = input.createdBy() != null ? parseUuidSafely(input.createdBy()) : null;

        ledgerServiceClient.postSettlementEntry(
                tenantId,
                loanId,
                input.loanNumber(),
                input.settlementAmount(),
                input.ibraAmount(),
                input.idempotencyKey(),
                createdBy
        );

        log.info("Settlement GL entry posted successfully: loanId={}", input.loanId());
        return new GlEntryResult(null, "POSTED", true);
    }

    private UUID parseUuidSafely(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            log.warn("Could not parse UUID from createdBy='{}', using null", value);
            return null;
        }
    }
}
