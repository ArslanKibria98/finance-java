package com.ksa.financing.lending.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for ledger-service GL journal entry operations.
 *
 * Ledger-service is the single bridge between domain services and Fineract GL.
 * All financial transactions that require GL entries (disbursement, repayment,
 * accrual, settlement) must call ledger-service — NOT Fineract directly.
 *
 * Zero hardcoding: all account codes and URLs come from ${ENV_VAR:default}.
 */
@Slf4j
@Component
public class LedgerServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ledgerServiceUrl;
    private final String loansReceivableAccount;
    private final String bankDisbursementAccount;
    private final String profitReceivableAccount;
    private final String deferredIncomeAccount;

    public LedgerServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.ledger-service-url}") String ledgerServiceUrl,
            @Value("${app.gl.accounts.loans-receivable:1200}") String loansReceivableAccount,
            @Value("${app.gl.accounts.bank-disbursement:1010}") String bankDisbursementAccount,
            @Value("${app.gl.accounts.profit-receivable:1300}") String profitReceivableAccount,
            @Value("${app.gl.accounts.deferred-income:2200}") String deferredIncomeAccount) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ledgerServiceUrl = ledgerServiceUrl;
        this.loansReceivableAccount = loansReceivableAccount;
        this.bankDisbursementAccount = bankDisbursementAccount;
        this.profitReceivableAccount = profitReceivableAccount;
        this.deferredIncomeAccount = deferredIncomeAccount;
    }

    // -----------------------------------------------------------------------
    // Loan Disbursement GL Entry
    // -----------------------------------------------------------------------

    /**
     * Post GL journal entries for a loan disbursement via ledger-service.
     *
     * Accounting entries:
     *   Dr. Loans Receivable (${app.gl.accounts.loans-receivable})   amount
     *   Cr. Bank / Cash Account (${app.gl.accounts.bank-disbursement}) amount
     *
     * @param tenantId       Tenant UUID from JWT
     * @param loanId         UUID of the loan in our system
     * @param loanNumber     Human-readable loan number
     * @param amount         Disbursement amount (SAR)
     * @param idempotencyKey Unique key — same key on retry returns cached result
     * @param createdBy      UUID of operator/system creating the entry
     */
    public void postDisbursementEntry(
            UUID tenantId,
            UUID loanId,
            String loanNumber,
            BigDecimal amount,
            String idempotencyKey,
            UUID createdBy) {

        log.info("Posting disbursement GL entry to ledger-service: loanId={} amount={} idempotencyKey={}",
                loanId, amount, idempotencyKey);

        var lines = List.of(
                buildLine(loansReceivableAccount, amount, BigDecimal.ZERO,
                        "Loans receivable — " + loanNumber),
                buildLine(bankDisbursementAccount, BigDecimal.ZERO, amount,
                        "Bank disbursement — " + loanNumber)
        );

        var request = buildJournalEntryRequest(
                LocalDate.now(),
                "DISBURSEMENT",
                loanId,
                "LOAN_DISBURSEMENT",
                "Loan disbursement for " + loanNumber,
                lines,
                idempotencyKey
        );

        postToLedgerService(request, tenantId, createdBy, "disbursement");
    }

    // -----------------------------------------------------------------------
    // Loan Repayment GL Entry
    // -----------------------------------------------------------------------

    /**
     * Post GL journal entries for a loan repayment via ledger-service.
     *
     * Accounting entries:
     *   Dr. Bank / Cash Account (${app.gl.accounts.bank-disbursement})  amount
     *   Cr. Loans Receivable (${app.gl.accounts.loans-receivable})        amount
     *
     * @param tenantId       Tenant UUID from JWT
     * @param loanId         UUID of the loan in our system
     * @param loanNumber     Human-readable loan number
     * @param amount         Repayment amount (SAR)
     * @param idempotencyKey Unique key — same key on retry returns cached result
     * @param createdBy      UUID of operator/system creating the entry
     */
    public void postRepaymentEntry(
            UUID tenantId,
            UUID loanId,
            String loanNumber,
            BigDecimal amount,
            String idempotencyKey,
            UUID createdBy) {

        log.info("Posting repayment GL entry to ledger-service: loanId={} amount={}", loanId, amount);

        var lines = List.of(
                buildLine(bankDisbursementAccount, amount, BigDecimal.ZERO,
                        "Repayment received — " + loanNumber),
                buildLine(loansReceivableAccount, BigDecimal.ZERO, amount,
                        "Loans receivable reduction — " + loanNumber)
        );

        var request = buildJournalEntryRequest(
                LocalDate.now(),
                "REPAYMENT",
                loanId,
                "LOAN_REPAYMENT",
                "Loan repayment for " + loanNumber,
                lines,
                idempotencyKey
        );

        postToLedgerService(request, tenantId, createdBy, "repayment");
    }

    // -----------------------------------------------------------------------
    // Restructuring Write-off GL Entry
    // -----------------------------------------------------------------------

    /**
     * Post GL journal entries for loan restructuring write-off via ledger-service.
     *
     * Accounting entries per Blueprint 17 § 4.4:
     *   Dr. Provision for Bad Debts (${app.gl.accounts.provision-bad-debts})   writeOffAmount
     *   Cr. Loans Receivable        (${app.gl.accounts.loans-receivable})       writeOffAmount
     *
     *   Dr. Deferred/Unearned Income (${app.gl.accounts.deferred-income})      profitWaiverAmount
     *   Cr. Loans Receivable Profit  (${app.gl.accounts.profit-receivable})    profitWaiverAmount
     */
    public void postRestructuringWriteOffEntry(
            UUID tenantId,
            UUID loanId,
            String loanNumber,
            BigDecimal writeOffAmount,
            BigDecimal profitWaiverAmount,
            String idempotencyKey,
            UUID createdBy) {

        log.info("Posting restructuring write-off GL entry: loanId={} writeOff={} waiver={}",
                loanId, writeOffAmount, profitWaiverAmount);

        var lines = new java.util.ArrayList<java.util.Map<String, Object>>();

        if (writeOffAmount != null && writeOffAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(buildLine("1400", writeOffAmount, BigDecimal.ZERO,
                    "Provision for bad debts — " + loanNumber));
            lines.add(buildLine(loansReceivableAccount, BigDecimal.ZERO, writeOffAmount,
                    "Principal write-off — " + loanNumber));
        }

        if (profitWaiverAmount != null && profitWaiverAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(buildLine(deferredIncomeAccount, profitWaiverAmount, BigDecimal.ZERO,
                    "Unearned profit waiver — " + loanNumber));
            lines.add(buildLine(profitReceivableAccount, BigDecimal.ZERO, profitWaiverAmount,
                    "Profit waiver on restructure — " + loanNumber));
        }

        if (lines.isEmpty()) {
            log.warn("No write-off or profit waiver amounts provided for restructuring GL entry: loanId={}", loanId);
            return;
        }

        var request = buildJournalEntryRequest(
                LocalDate.now(),
                "RESTRUCTURING",
                loanId,
                "LOAN_RESTRUCTURING_WRITEOFF",
                "Loan restructuring write-off for " + loanNumber,
                lines,
                idempotencyKey
        );

        postToLedgerService(request, tenantId, createdBy, "restructuring write-off");
    }

    /**
     * Post GL journal entries for a loan settlement (early or final) via ledger-service.
     *
     * Accounting entries per Blueprint 17 § 3.1:
     *   Dr. Bank / Cash Account (${app.gl.accounts.bank-disbursement})  settlementAmount
     *   Dr. Deferred/Unearned Income (${app.gl.accounts.deferred-income})  ibraAmount
     *   Cr. Loans Receivable (${app.gl.accounts.loans-receivable})        settlementAmount + ibraAmount
     */
    public void postSettlementEntry(
            UUID tenantId,
            UUID loanId,
            String loanNumber,
            BigDecimal settlementAmount,
            BigDecimal ibraAmount,
            String idempotencyKey,
            UUID createdBy) {

        log.info("Posting settlement GL entry to ledger-service: loanId={} amount={} ibra={}",
                loanId, settlementAmount, ibraAmount);

        var totalCredit = settlementAmount.add(ibraAmount != null ? ibraAmount : BigDecimal.ZERO);

        var lines = new java.util.ArrayList<java.util.Map<String, Object>>();
        lines.add(buildLine(bankDisbursementAccount, settlementAmount, BigDecimal.ZERO,
                "Settlement payment received — " + loanNumber));

        if (ibraAmount != null && ibraAmount.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(buildLine(deferredIncomeAccount, ibraAmount, BigDecimal.ZERO,
                    "Ibra (profit waiver) on settlement — " + loanNumber));
        }

        lines.add(buildLine(loansReceivableAccount, BigDecimal.ZERO, totalCredit,
                "Loans receivable closure — " + loanNumber));

        var request = buildJournalEntryRequest(
                LocalDate.now(),
                "SETTLEMENT",
                loanId,
                "LOAN_SETTLEMENT",
                "Loan settlement for " + loanNumber,
                lines,
                idempotencyKey
        );

        postToLedgerService(request, tenantId, createdBy, "settlement");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void postToLedgerService(Object request, UUID tenantId, UUID createdBy, String entryType) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());
            headers.set("X-Caller-Service", "lending-service");
            if (createdBy != null) {
                headers.set("X-Created-By", createdBy.toString());
            }

            var requestBody = objectMapper.writeValueAsString(request);
            var url = ledgerServiceUrl + "/internal/v1/journal-entries";

            var response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("GL {} entry posted successfully to ledger-service", entryType);
            } else {
                log.error("Ledger-service returned non-2xx for {} entry: status={}", entryType,
                        response.getStatusCode());
                throw new IllegalStateException("Ledger-service returned " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Ledger-service rejected {} GL entry: status={} body={}",
                    entryType, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Failed to post {} GL entry to ledger-service: {}", entryType, e.getMessage(), e);
            throw new IllegalStateException("Failed to post " + entryType + " GL entry: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildLine(String accountCode, BigDecimal debit, BigDecimal credit, String description) {
        return Map.of(
                "accountCode", accountCode,
                "debitAmount", debit,
                "creditAmount", credit,
                "description", description
        );
    }

    private Map<String, Object> buildJournalEntryRequest(
            LocalDate entryDate,
            String referenceType,
            UUID referenceId,
            String transactionType,
            String description,
            List<Map<String, Object>> lines,
            String idempotencyKey) {
        return Map.of(
                "entryDate", entryDate.toString(),
                "referenceType", referenceType,
                "referenceId", referenceId.toString(),
                "transactionType", transactionType,
                "description", description,
                "lines", lines,
                "idempotencyKey", idempotencyKey
        );
    }
}
