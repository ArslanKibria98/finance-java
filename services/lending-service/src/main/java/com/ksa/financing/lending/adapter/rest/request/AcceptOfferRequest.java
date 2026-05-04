package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Step 4: Accept or reject the financing offer")
public record AcceptOfferRequest(

        @NotNull
        @Schema(description = "Whether customer accepts the offer")
        Boolean accepted,

        @Schema(description = "Selected amount (can be <= maxEligibleAmount)")
        BigDecimal selectedAmount
) {}
