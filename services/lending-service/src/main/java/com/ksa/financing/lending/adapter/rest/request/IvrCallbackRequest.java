package com.ksa.financing.lending.adapter.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Step 5c: IVR call verification callback")
public record IvrCallbackRequest(

        @NotNull
        @Schema(description = "Whether IVR verification was successful")
        Boolean verified,

        @Schema(description = "IVR call ID")
        String callId,

        @Schema(description = "Verification status from IVR system")
        String verificationStatus
) {}
