package com.ksa.financing.wallet.domain.port.in;

import java.util.UUID;

/**
 * Validate a destination account number. Shared by two flows, selected by {@code type}:
 *  - {@code type = "phone"} (FT): {@code accountNumber} carries the recipient's MOBILE number.
 *    The customer is verified by mobile; their wallet account number is sent to Scotia.
 *    Rejected (BusinessException) if no matching customer / wallet.
 *  - {@code type = "account"} (IBFT): {@code accountNumber} is a real bank account number and is
 *    sent straight to Scotia as-is (no customer lookup, no own-wallet gate).
 *  - {@code type} null/blank → auto-detected from the value format.
 * The transit is derived from the first 5 digits of the resolved account number (non-digits
 * ignored). {@code fullName} is optional.
 */
public interface ValidateAccountUseCase {

    ValidationResult validate(UUID tenantId, String accountNumber, String institutionNumber,
                              String fullName, String currency, String type);

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
