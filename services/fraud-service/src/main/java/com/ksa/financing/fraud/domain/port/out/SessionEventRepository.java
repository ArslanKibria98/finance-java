package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.session.SessionEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionEventRepository {

    SessionEvent save(SessionEvent event);

    Optional<SessionEvent> findLastByCustomer(UUID tenantId, String customerId);

    List<SessionEvent> findByCustomerSince(UUID tenantId, String customerId, LocalDateTime since);

    long countByCustomerSince(UUID tenantId, String customerId, LocalDateTime since);

    Optional<SessionEvent> findLastByCustomerAndDevice(UUID tenantId, String customerId, String deviceId);

    long countDistinctCustomersByDevice(UUID tenantId, String deviceId, LocalDateTime since);
}
