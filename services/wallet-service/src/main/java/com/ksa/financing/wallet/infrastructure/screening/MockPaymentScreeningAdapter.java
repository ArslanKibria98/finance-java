package com.ksa.financing.wallet.infrastructure.screening;

import com.ksa.financing.wallet.domain.port.out.PaymentScreeningPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Mock AML / sanctions screening adapter for development.
 * <p>
 * Deterministic test rules:
 *   - beneficiary name in SANCTIONS_LIST           → REJECT
 *   - beneficiary name in PEP_LIST                  → HOLD_FOR_REVIEW
 *   - beneficiary IBAN in HIGH_RISK_BLOCKED         → REJECT
 *   - amount >= edd-threshold (default 50000)       → HOLD_FOR_REVIEW (Enhanced DD)
 *   - destination country in HIGH_RISK_CORRIDORS    → HOLD_FOR_REVIEW
 *   - otherwise                                      → PASS
 * <p>
 * Replace with real adapter calling risk-service /api/v1/risk/screen-payment in production.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ksa.wallet.screening.mock", havingValue = "true", matchIfMissing = true)
public class MockPaymentScreeningAdapter implements PaymentScreeningPort {

    /** Demo names that trigger sanctions REJECT. */
    private static final Set<String> SANCTIONS_LIST = Set.of(
            "OSAMA BIN LADEN",
            "SANCTIONED PERSON",
            "BLOCKED ENTITY",
            "OFAC TEST"
    );

    /** Demo names that trigger PEP HOLD. */
    private static final Set<String> PEP_LIST = Set.of(
            "PEP TEST",
            "POLITICAL FIGURE",
            "MINISTER TEST"
    );

    /** Demo IBANs that trigger REJECT (override even valid checksum). */
    private static final Set<String> HIGH_RISK_BLOCKED_IBAN_PREFIXES = Set.of(
            "SA9999"
    );

    /** Country codes deemed high-risk for STR review. */
    private static final Set<String> HIGH_RISK_CORRIDORS = Set.of(
            "IR",  // Iran
            "KP",  // North Korea
            "SY",  // Syria
            "CU"   // Cuba
    );

    @Value("${ksa.wallet.screening.edd-threshold:50000}")
    private BigDecimal eddThreshold;

    @Override
    public ScreeningResult screen(ScreeningRequest req) {
        log.info("[MOCK-SCREEN] Screening payment customerId={} beneficiary={} country={} amount={}",
                req.customerId(),
                req.beneficiaryName() != null ? req.beneficiaryName().toUpperCase() : "?",
                req.beneficiaryCountry(),
                req.amount());

        String beneficiaryUpper = req.beneficiaryName() != null
                ? req.beneficiaryName().toUpperCase().trim()
                : "";
        String iban = req.beneficiaryIban() != null
                ? req.beneficiaryIban().toUpperCase()
                : "";
        String country = req.beneficiaryCountry() != null
                ? req.beneficiaryCountry().toUpperCase()
                : "";

        String screeningRef = "SCRN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Instant now = Instant.now();

        // 1. Sanctions list (hard reject)
        if (SANCTIONS_LIST.contains(beneficiaryUpper)) {
            log.warn("[MOCK-SCREEN] REJECT — sanctions match: {}", beneficiaryUpper);
            return new ScreeningResult(
                    Decision.REJECT, 100,
                    List.of(new Match("OFAC", beneficiaryUpper, 100, "Exact sanctions list match")),
                    false, screeningRef, now);
        }

        // 2. High-risk IBAN prefix (hard reject)
        for (String badPrefix : HIGH_RISK_BLOCKED_IBAN_PREFIXES) {
            if (iban.startsWith(badPrefix)) {
                log.warn("[MOCK-SCREEN] REJECT — IBAN blocked: {}", badPrefix);
                return new ScreeningResult(
                        Decision.REJECT, 95,
                        List.of(new Match("SAMA", iban, 100, "IBAN on internal blocklist")),
                        false, screeningRef, now);
            }
        }

        // 3. PEP match (hold)
        if (PEP_LIST.contains(beneficiaryUpper)) {
            log.warn("[MOCK-SCREEN] HOLD — PEP match: {}", beneficiaryUpper);
            return new ScreeningResult(
                    Decision.HOLD_FOR_REVIEW, 75,
                    List.of(new Match("PEP", beneficiaryUpper, 90, "Politically Exposed Person")),
                    true, screeningRef, now);
        }

        // 4. High-risk corridor (hold)
        if (HIGH_RISK_CORRIDORS.contains(country)) {
            log.warn("[MOCK-SCREEN] HOLD — high-risk corridor: {}", country);
            return new ScreeningResult(
                    Decision.HOLD_FOR_REVIEW, 70,
                    List.of(new Match("CORRIDOR", country, 100, "Sanctioned country: " + country)),
                    true, screeningRef, now);
        }

        // 5. Enhanced Due Diligence threshold (hold)
        if (eddThreshold != null && req.amount() != null
                && req.amount().compareTo(eddThreshold) >= 0) {
            log.info("[MOCK-SCREEN] HOLD — EDD threshold ({})", eddThreshold);
            return new ScreeningResult(
                    Decision.HOLD_FOR_REVIEW, 60,
                    List.of(),
                    true, screeningRef, now);
        }

        // 6. Pass
        log.info("[MOCK-SCREEN] PASS ref={}", screeningRef);
        return new ScreeningResult(
                Decision.PASS, computeRiskScore(req),
                List.of(),
                false, screeningRef, now);
    }

    private int computeRiskScore(ScreeningRequest req) {
        int score = 10;  // base
        if (req.amount() != null && req.amount().compareTo(new BigDecimal("10000")) >= 0) score += 15;
        if (req.amount() != null && req.amount().compareTo(new BigDecimal("25000")) >= 0) score += 10;
        return Math.min(score, 100);
    }
}
