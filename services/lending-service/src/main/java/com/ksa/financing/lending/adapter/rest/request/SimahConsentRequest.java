package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Step 3: Give SIMAH consent for credit check")
public record SimahConsentRequest(

        @Schema(description = "Whether customer gives consent for SIMAH credit check")
        boolean consentGiven
) {}
