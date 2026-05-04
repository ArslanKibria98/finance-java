package com.ksa.financing.collections.adapter.rest.response;

import java.util.UUID;

public record WriteOffEligibilityResponse(
        UUID loanId,
        int eligibleCount,
        String asOf
) {}
