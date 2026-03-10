package com.ksa.financing.onboarding.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NafathCallbackSignal(
    @JsonProperty("nationalId") String nationalId,
    @JsonProperty("transactionId") String transactionId,
    @JsonProperty("accepted") boolean accepted,
    @JsonProperty("rejectionReason") String rejectionReason
) {}
