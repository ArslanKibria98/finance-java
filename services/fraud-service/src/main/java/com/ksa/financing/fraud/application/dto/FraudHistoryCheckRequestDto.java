package com.ksa.financing.fraud.application.dto;

public record FraudHistoryCheckRequestDto(
        String nationalIdHash,
        String mobileHash,
        String customerId
) {}
