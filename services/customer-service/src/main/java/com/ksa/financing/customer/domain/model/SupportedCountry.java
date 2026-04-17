package com.ksa.financing.customer.domain.model;

/**
 * Domain model for a supported country with its KYC configuration.
 */
public record SupportedCountry(
        String countryCode,
        String countryName,
        String countryNameAr,
        String currencyCode,
        boolean active,
        String idTypes,
        String defaultIdType,
        String kycProviders,
        String flagEmoji,
        String dialCode,
        String nationalityEn,
        String nationalityAr
) {}
