package com.ksa.financing.product.domain.model;

import java.util.List;
import java.util.UUID;

public record ApprovalConditionFieldDefinition(
    UUID id,
    String fieldKey,
    String nameEn,
    String nameAr,
    String dataType,
    boolean active,
    int sortOrder,
    List<ApprovalConditionFieldOption> options
) {}
