package com.ksa.financing.notification.domain.repository;

import com.ksa.financing.notification.domain.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    // findFirst (not a plain findBy) so a transient duplicate GLOBAL row — created in the
    // tiny window before the uq_prefs_global_customer index commits under concurrent inserts —
    // can never throw IncorrectResultSizeDataAccessException and abort event processing.
    Optional<NotificationPreference> findFirstByTenantIdAndCustomerIdOrderByCreatedAtAsc(UUID tenantId, UUID customerId);
}
