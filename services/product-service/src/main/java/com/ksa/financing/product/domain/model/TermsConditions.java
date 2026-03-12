package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record TermsConditions(
    UUID id,
    String termsEn,
    String termsAr
) {}
