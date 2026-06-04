package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.Size;

/** Admin decision payload for approve / reject of a limit-change request. */
public record DecideLimitChangeRequest(
        @Size(max = 500) String notes,
        @Size(max = 500) String rejectionReason) {
}
