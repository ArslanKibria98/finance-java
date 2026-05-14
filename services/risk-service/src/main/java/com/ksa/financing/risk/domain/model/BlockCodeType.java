package com.ksa.financing.risk.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category of the block code")
public enum BlockCodeType {
    COMPLIANCE,
    AML,
    ANTI_FRAUD,
    SANCTION
}
