package com.ksa.financing.onboarding.guest.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestVerifyOtpRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[0-9]{4,8}$") String mobileOtp,
        @NotBlank @Pattern(regexp = "^[0-9]{4,8}$") String emailOtp
) {
}
