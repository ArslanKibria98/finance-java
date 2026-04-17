package com.ksa.financing.product.domain.model;

import java.util.UUID;

public record ApprovalConditionFieldOption(
    UUID id,
    String optionKey,
    String labelEn,
    String labelAr,
    int sortOrder
) {}
