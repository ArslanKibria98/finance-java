package com.ksa.financing.customer.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    String cifNumber,
    String customerType,
    String nationalId,
    String nationalIdType,
    String firstName,
    String lastName,
    String firstNameAr,
    String lastNameAr,
    String fullName,
    LocalDate dateOfBirth,
    String gender,
    String nationality,
    String residencyType,
    String mobileNumber,
    String email,
    String kycStatus,
    String lifecycleStage,
    String riskGrade,
    boolean pepFlag,
    boolean sanctionsFlag,
    UUID globalUid,
    String profilePicture,
    Instant createdAt,
    Instant updatedAt
) {}
