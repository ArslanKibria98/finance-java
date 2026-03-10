package com.ksa.financing.customer.application.dto;

public record UpdateCustomerRequest(
    String email,
    String mobileNumber,
    String addressLine1,
    String addressLine2,
    String city,
    String region,
    String postalCode
) {}
