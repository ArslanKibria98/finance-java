package com.ksa.financing.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "template_routing_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRoutingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "rule_code", unique = true, nullable = false)
    private String ruleCode;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_channel")
    private NotificationChannel channel;

    @Column(name = "novu_template_id", nullable = false)
    private String novuTemplateId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "notification_priority")
    private Priority priority = Priority.NORMAL;

    @Column(name = "is_active")
    private boolean active = true;

    public enum NotificationChannel {
        SMS, EMAIL, PUSH, WHATSAPP, IN_APP
    }

    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }
}
