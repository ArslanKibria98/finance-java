package com.ksa.financing.piivault.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StorePiiRequest(
    @NotNull UUID globalUid,
    @NotBlank String nationalId,
    @NotBlank String nationalIdType,
    @NotBlank String fullName,
    @NotBlank String firstName,
    String middleName,
    @NotBlank String lastName,
    String fullNameAr,
    @NotBlank String dateOfBirth,
    String gender,
    @NotBlank String nationalityCode,
    @NotBlank String mobile,
    String email,
    @NotBlank String countryCode
) {}
