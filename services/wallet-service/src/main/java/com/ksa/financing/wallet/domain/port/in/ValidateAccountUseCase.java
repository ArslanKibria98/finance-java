package com.ksa.financing.wallet.domain.port.in;

import java.util.UUID;

/**
 * Validate a destination account number.
 *
 * Rules:
 *  - The account number MUST exist in our own wallets (exact {@code account_number} match)
 *    for the tenant. If not found → result is {@code NO_MATCH_FOUND} (Scotia is NOT called).
 *  - When found, the transit is derived from the first 5 digits of the account number
 *    (non-digits ignored) and a Scotia account-validation is performed.
 *  - {@code fullName} is optional.
 */
public interface ValidateAccountUseCase {

    ValidationResult validate(UUID tenantId, String accountNumber, String institutionNumber,
                              String fullName, String currency);

    record ValidationResult(
            boolean valid,
            String status,            // VALID | INVALID | NO_MATCH_FOUND
            String accountNumber,
            String transit,           // derived (first 5 digits), null when no match
            String institutionNumber,
            String message,
            String scotiaRef,
            String scotiaRaw          // raw Scotia provider data (JSON string), null when no match
    ) {}
}
