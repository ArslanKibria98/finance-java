package com.ksa.financing.fraud.domain.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerProfilePort {

    Optional<CustomerProfileSnapshot> fetchProfile(UUID tenantId, String customerId);

    record CustomerProfileSnapshot(
        String customerId,
        String fullName,
        String registeredCity,
        String registeredCountry,
        BigDecimal nationalAddressLatitude,
        BigDecimal nationalAddressLongitude,
        String registeredIban,
        List<String> registeredCardTokens
    ) {}
}
