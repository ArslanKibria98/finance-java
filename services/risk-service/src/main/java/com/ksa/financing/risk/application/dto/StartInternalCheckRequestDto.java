package com.ksa.financing.risk.application.dto;

/**
 * Request DTO for internal risk checks.
 *
 * Supports 3 input modes:
 *   1. NID only:     { "nationalId": "1234567890", "countryCode": "SAU" }
 *   2. Mobile only:  { "mobileNumber": "+966501234567", "countryCode": "SAU" }
 *   3. Both:         { "nationalId": "1234567890", "mobileNumber": "+966501234567", "countryCode": "SAU" }
 *
 * At least one of nationalId or mobileNumber must be provided.
 * countryCode defaults to SAU if not provided.
 *
 * Country-wise NID validation:
 *   SAU: 10 digits, starts with 1 (citizen) or 2 (resident/iqama)
 *   PAK: 13 digits (CNIC format: XXXXX-XXXXXXX-X)
 *   ARE: 15 digits (Emirates ID: 784-XXXX-XXXXXXX-X)
 */
public record StartInternalCheckRequestDto(
    String nationalId,
    String mobileNumber,
    String countryCode
) {
    public String resolvedCountryCode() {
        return countryCode != null && !countryCode.isBlank() ? countryCode.toUpperCase().trim() : "SAU";
    }

    public boolean hasNationalId() {
        return nationalId != null && !nationalId.isBlank();
    }

    public boolean hasMobileNumber() {
        return mobileNumber != null && !mobileNumber.isBlank();
    }
}
