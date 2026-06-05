package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.iso.ExternalPurpose1Code;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbound port for AML / sanctions / PEP screening of an outgoing payment.
 * <p>
 * Called BEFORE the Fineract debit. SAMA requires sanctions screening on every
 * outbound payment regardless of amount. The decision controls SAGA flow:
 * <ul>
 *   <li>{@link Decision#PASS}              — proceed with debit</li>
 *   <li>{@link Decision#HOLD_FOR_REVIEW}    — stop, status=HELD_AML, await compliance officer</li>
 *   <li>{@link Decision#REJECT}             — stop, status=FAILED, return error</li>
 * </ul>
 */
public interface PaymentScreeningPort {

    ScreeningResult screen(ScreeningRequest request);

    record ScreeningRequest(
            UUID    tenantId,
            UUID    customerId,
            String  beneficiaryName,
            String  beneficiaryIban,
            String  beneficiaryCountry,
            BigDecimal amount,
            String  currency,
            ExternalPurpose1Code purposeCode
    ) {}

    record ScreeningResult(
            Decision      decision,
            int           riskScore,        // 0-100
            List<Match>   matches,
            boolean       requiresEdd,      // Enhanced Due Diligence
            String        screeningRef,
            Instant       screenedAt
    ) {}

    enum Decision { PASS, HOLD_FOR_REVIEW, REJECT }

    record Match(
            String listType,    // OFAC, UN, SAMA, PEP, ADVERSE_MEDIA
            String matchedName,
            int    matchScore,  // fuzzy match confidence 0-100
            String reason
    ) {}
}
