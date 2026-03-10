package com.ksa.financing.risk.domain.model.fraud;

import com.ksa.financing.risk.domain.model.device.DeviceInfo;
import com.ksa.financing.risk.domain.model.location.LocationData;
import com.ksa.financing.risk.domain.model.transaction.PaymentSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record FraudEvent(
    UUID id,
    UUID tenantId,
    String eventId,
    FraudEventType eventType,
    String customerId,
    String nationalIdHash,
    // Device Data (HLD Table 3)
    DeviceInfo deviceInfo,
    // Location Data (HLD Table 4)
    LocationData locationData,
    // Session Data (HLD Table 5)
    LocalDateTime eventTimestamp,
    String sessionId,
    // Transaction Data (HLD Table 7)
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
    PaymentSource paymentSource,
    // Enrichment (populated by enrichment pipeline)
    String resolvedCountry,
    String resolvedCity,
    BigDecimal distanceFromLastKm,
    Duration timeSinceLastLogin,
    boolean vpnDetected,
    boolean proxyDetected,
    // Metadata
    LocalDateTime receivedAt,
    String correlationId
) {}
