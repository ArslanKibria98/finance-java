package com.ksa.financing.fraud.infrastructure.adapter;

import com.ksa.financing.fraud.domain.port.out.CustomerProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class StubCustomerProfileAdapter implements CustomerProfilePort {

    @Override
    public Optional<CustomerProfileSnapshot> fetchProfile(UUID tenantId, String customerId) {
        log.debug("Stub fetchProfile: tenantId={} customerId={} → empty", tenantId, customerId);
        return Optional.empty();
    }
}
