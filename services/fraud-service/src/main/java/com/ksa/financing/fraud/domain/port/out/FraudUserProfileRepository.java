package com.ksa.financing.fraud.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface FraudUserProfileRepository {

    Optional<FraudUserProfileView> findByCustomerId(UUID tenantId, String customerId);

    void updateLastLogin(UUID tenantId, String customerId, LocalDateTime loginAt,
                         BigDecimal latitude, BigDecimal longitude,
                         String country, String city, String deviceId);

    void incrementLoanApplicationCount(UUID tenantId, String customerId);

    void updateLastActivity(UUID tenantId, String customerId, LocalDateTime activityAt);

    record FraudUserProfileView(
        String customerId,
        String nationalIdHash,
        String nationality,
        String registeredAddressCity,
        String registeredAddressCountry,
        String registeredIban,
        boolean ibanVerified,
        String ibanHolderName,
        String amlRiskLevel,
        LocalDateTime lastLoginAt,
        BigDecimal lastLoginLatitude,
        BigDecimal lastLoginLongitude,
        String lastLoginCountry,
        String lastLoginCity,
        String lastLoginDeviceId,
        LocalDateTime accountCreatedAt,
        LocalDateTime lastActivityAt,
        int totalLoanApplications,
        int totalDisbursements,
        int totalRepayments,
        boolean dormant
    ) {}
}
