package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AddPartnerAffiliationRequest(
    @NotNull UUID partnerId,
    @NotBlank String affiliationType,
    @NotNull BigDecimal commissionPercentage
) {}
