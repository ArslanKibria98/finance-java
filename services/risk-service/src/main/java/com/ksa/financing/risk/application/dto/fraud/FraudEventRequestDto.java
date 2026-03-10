package com.ksa.financing.risk.application.dto.fraud;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FraudEventRequestDto(
    @NotBlank String eventId,
    @NotNull String eventType,
    @NotBlank String customerId,
    String nationalIdHash,
    // Device Data
    String deviceId,
    String deviceType,
    String deviceOs,
    String osVersion,
    String deviceFingerprint,
    String deviceIntegrity,
    // Location Data
    BigDecimal latitude,
    BigDecimal longitude,
    String ipAddress,
    String ipCountry,
    String ipCity,
    String gpsCountry,
    String gpsCity,
    Boolean vpnDetected,
    Boolean proxyDetected,
    // Session Data
    String sessionId,
    @NotNull LocalDateTime eventTimestamp,
    // Transaction Data
    String transactionType,
    BigDecimal transactionAmount,
    String currency,
    // Loan-specific
    String loanApplicationId,
    String loanProductType,
    BigDecimal approvedLoanAmount,
    // IBAN-specific
    String disbursementIban,
    String ibanVerificationStatus,
    String ibanHolderName,
    // Payment-specific
    String paymentIban,
    String cardLast4,
    String cardCountry,
    String cardHolderName,
    Boolean thirdPartyPayment,
    // Metadata
    String correlationId
) {}
