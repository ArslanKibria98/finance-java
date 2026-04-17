package com.ksa.financing.customer.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateCustomerRequest(
    String firstName,
    String lastName,
    String firstNameAr,
    String lastNameAr,
    String email,
    String mobileNumber,
    String addressLine1,
    String addressLine2,
    String city,
    String region,
    String postalCode,
    String profilePicture
) {}
