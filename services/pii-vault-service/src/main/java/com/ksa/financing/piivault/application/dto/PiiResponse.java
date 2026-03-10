package com.ksa.financing.piivault.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PiiResponse(
    UUID piiId,
    UUID globalUid,
    String nationalId,
    String nationalIdType,
    String fullName,
    String firstName,
    String lastName,
    String fullNameAr,
    String dateOfBirth,
    String gender,
    String nationalityCode,
    String mobile,
    String email,
    String countryCode,
    Instant createdAt
) {}
