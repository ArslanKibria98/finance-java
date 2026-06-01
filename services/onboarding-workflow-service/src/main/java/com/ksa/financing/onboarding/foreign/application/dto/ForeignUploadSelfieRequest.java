package com.ksa.financing.onboarding.foreign.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForeignUploadSelfieRequest(
        @NotBlank @Email String email,
        @NotBlank String selfieImageBase64
) {
}
