package com.ksa.financing.onboarding.canada.application.dto;

import com.ksa.financing.onboarding.canada.domain.model.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CanadaSelectDocumentRequest(
        @NotBlank @Email String email,
        @NotNull DocumentType documentType
) {
}
