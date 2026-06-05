package com.ksa.financing.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage = "en";

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "sms_enabled")
    private boolean smsEnabled = true;

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    // Read-only: populated by the DB default (NOW()). Mapped so queries can order by it
    // (findFirst...OrderByCreatedAtAsc). JPA never writes it.
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
