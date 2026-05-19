package com.ksa.financing.wallet.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CheckRecipientsRequest(
        @NotEmpty
        @Size(max = 500)
        List<String> mobileNumbers
) {}
