package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record CreditScoringFieldOption(
    UUID id,
    String optionKey,
    String labelEn,
    String labelAr,
    int sortOrder
) {}
