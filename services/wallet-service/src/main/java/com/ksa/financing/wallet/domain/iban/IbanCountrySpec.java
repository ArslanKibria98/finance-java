package com.ksa.financing.wallet.domain.iban;

import java.util.Map;
import java.util.Optional;

/**
 * IBAN length per country (ISO 13616 registry, partial — GCC + key markets).
 * Length includes the 2-char country code + 2-char check digits.
 */
public final class IbanCountrySpec {

    private static final Map<String, Integer> LENGTHS = Map.ofEntries(
            // GCC
            Map.entry("SA", 24),   // Saudi Arabia
            Map.entry("AE", 23),   // UAE
            Map.entry("KW", 30),   // Kuwait
            Map.entry("BH", 22),   // Bahrain
            Map.entry("QA", 29),   // Qatar
            Map.entry("OM", 23),   // Oman
            // MENA
            Map.entry("EG", 29),   // Egypt
            Map.entry("JO", 30),   // Jordan
            Map.entry("LB", 28),   // Lebanon
            Map.entry("PS", 29),   // Palestine
            Map.entry("TR", 26),   // Turkey
            // Other commonly-encountered
            Map.entry("GB", 22),   // United Kingdom
            Map.entry("DE", 22),   // Germany
            Map.entry("FR", 27),   // France
            Map.entry("IT", 27),   // Italy
            Map.entry("ES", 24),   // Spain
            Map.entry("CH", 21),   // Switzerland
            Map.entry("US", 0),    // US has no IBAN — placeholder, will fail validation
            Map.entry("PK", 24),   // Pakistan
            Map.entry("IN", 0)     // India — no IBAN
    );

    private IbanCountrySpec() {}

    public static Optional<Integer> expectedLength(String countryCode) {
        if (countryCode == null) return Optional.empty();
        Integer len = LENGTHS.get(countryCode.toUpperCase());
        return (len == null || len == 0) ? Optional.empty() : Optional.of(len);
    }

    public static boolean isSupported(String countryCode) {
        return expectedLength(countryCode).isPresent();
    }
}
