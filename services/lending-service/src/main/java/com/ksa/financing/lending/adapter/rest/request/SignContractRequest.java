package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Step 5a: Sign contract with authorizations")
public record SignContractRequest(

        @Schema(description = "Authorize digital signature")
        boolean authorizeDigitalSignature,

        @Schema(description = "Authorize commodity sale (Tawarruq)")
        boolean authorizeSellCommodity,

        @Schema(description = "Want physical delivery of commodity")
        boolean wantPhysicalDelivery
) {}
