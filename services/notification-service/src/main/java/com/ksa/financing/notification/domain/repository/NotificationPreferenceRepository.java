package com.ksa.financing.notification.domain.repository;

import com.ksa.financing.notification.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    Optional<NotificationPreference> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
