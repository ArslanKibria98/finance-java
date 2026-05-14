package com.ksa.financing.wallet.infrastructure.bank;

import com.ksa.financing.wallet.domain.port.out.BankRailsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock SAMA SARIE / bank-rails adapter for development and demo.
 * <p>
 * Simulates a bank network response with deterministic rules:
 *   - Amount > {@code mock.bank-rails.reject-above} → REJECTED
 *   - Beneficiary IBAN starts with "SA00" → REJECTED (test pattern)
 *   - Otherwise → ACCEPTED with synthetic bank reference
 * <p>
 * Replace with a real SARIE adapter behind a circuit breaker for production.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ksa.wallet.bank-rails.mock", havingValue = "true", matchIfMissing = true)
public class MockSarieBankRailsAdapter implements BankRailsPort {

    @Value("${ksa.wallet.bank-rails.reject-above:50000}")
    private BigDecimal rejectAbove;

    @Override
    public SubmissionReceipt submit(SubmissionRequest req) {
        log.info("[MOCK-SARIE] Submitting withdrawalNumber={} amount={} iban={}",
                req.withdrawalNumber(), req.amount(), maskIban(req.destinationIban()));

        if (req.destinationIban() != null && req.destinationIban().startsWith("SA00")) {
            log.warn("[MOCK-SARIE] REJECT (test-pattern IBAN) withdrawalNumber={}", req.withdrawalNumber());
            return new SubmissionReceipt(false, null, null,
                    "REJECTED", "BANK.IBAN_INVALID",
                    "Beneficiary IBAN rejected by network (test pattern)");
        }

        if (rejectAbove != null && req.amount().compareTo(rejectAbove) > 0) {
            log.warn("[MOCK-SARIE] REJECT (amount > {}) withdrawalNumber={}", rejectAbove, req.withdrawalNumber());
            return new SubmissionReceipt(false, null, null,
                    "REJECTED", "BANK.LIMIT_EXCEEDED",
                    "Amount exceeds mock-network ceiling: " + rejectAbove);
        }

        String bankRef = "MOCK-BANK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String sarieRef = "SARIE-" + System.currentTimeMillis();
        log.info("[MOCK-SARIE] ACCEPT withdrawalNumber={} bankRef={} sarieRef={}",
                req.withdrawalNumber(), bankRef, sarieRef);

        return new SubmissionReceipt(true, bankRef, sarieRef,
                "SETTLED", null, null);
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 8) return "****";
        return iban.substring(0, 4) + "..." + iban.substring(iban.length() - 4);
    }
}
