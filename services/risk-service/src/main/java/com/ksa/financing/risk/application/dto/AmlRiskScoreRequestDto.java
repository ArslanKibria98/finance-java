package com.ksa.financing.risk.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Request DTO for AML risk score calculation via REST API.
 */
public record AmlRiskScoreRequestDto(
    @NotBlank(message = "nationalIdHash is required")
    String nationalIdHash,

    String nationality,

    String cityName,

    String occupationCode,

    BigDecimal monthlyIncome,

    String sourceOfIncome,

    String productRiskTier,

    boolean isPep,

    boolean isOnInternalList,

    String customerId,

    String idempotencyKey
) {}
