package com.ksa.financing.collections.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record ReverseWriteOffRequest(
        @NotBlank String reason
) {}
