package com.ksa.financing.ledger.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a GL account name.
 */
public record UpdateAccountRequest(

        @NotBlank(message = "Account name is required")
        @Size(max = 255, message = "Account name must be at most 255 characters")
        String accountName,

        @Size(max = 255, message = "Arabic account name must be at most 255 characters")
        String accountNameAr
) {}
