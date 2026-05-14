package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseHeldWithdrawalRequest(
        @NotBlank(message = "release reason is required for audit trail")
        @Size(max = 1000) String reason
) {}
