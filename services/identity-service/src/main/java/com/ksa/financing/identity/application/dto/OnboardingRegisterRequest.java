package com.ksa.financing.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record OnboardingRegisterRequest(
    @NotBlank String nationalId,
    @NotBlank String mobileNumber,
    String globalUid,
    String firstName,
    String fcmToken
) {}
