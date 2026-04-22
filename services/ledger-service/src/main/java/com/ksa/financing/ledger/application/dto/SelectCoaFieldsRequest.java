package com.ksa.financing.ledger.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SelectCoaFieldsRequest(
        @NotEmpty List<@NotBlank @Size(max = 100) String> fieldKeys
) {}
