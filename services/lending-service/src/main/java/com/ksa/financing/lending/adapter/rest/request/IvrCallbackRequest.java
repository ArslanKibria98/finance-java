package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Step 5c: IVR call verification callback")
public record IvrCallbackRequest(

        @Schema(description = "Whether IVR verification was successful")
        boolean verified,

        @Schema(description = "IVR call ID")
        String callId,

        @Schema(description = "Verification status from IVR system")
        String verificationStatus
) {}
