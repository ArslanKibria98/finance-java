package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateContractTemplateRequest(
    @NotBlank @Size(max = 200) String nameEn,
    @Size(max = 200) String nameAr,
    @NotNull UUID productId,
    @NotNull UUID typeId,
    @NotBlank @Size(max = 10) String language,
    String message
) {}
