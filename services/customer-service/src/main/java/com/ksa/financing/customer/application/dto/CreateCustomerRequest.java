package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateCustomerRequest(
    @NotBlank String nationalId,
    String nationalIdType,
    String firstName,
    String middleName,
    String lastName,
    String firstNameAr,
    String lastNameAr,
    String fullNameEn,
    String fullNameAr,
    LocalDate dateOfBirth,
    String gender,
    String nationality,
    String residencyType,
    String mobileNumber,
    String email,
    String lifecycleStage,
    String tenantId,
    String globalUid,
    String customerId,
    String idempotencyKey
) {}
