package com.ksa.financing.identity.domain.port.out;

import java.util.UUID;

public interface EventPublisherPort {
    void publishUserRegistered(UUID userId, UUID tenantId, String fcmToken);
    void publishUserStatusChanged(UUID userId, String fromStatus, String toStatus);
    void publishUserLogin(UUID userId, UUID tenantId, UUID customerId, String mobileNumber, String name);
}
