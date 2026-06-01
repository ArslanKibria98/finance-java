package com.ksa.financing.notification.application.service;

import com.ksa.financing.notification.domain.model.NotificationPreference;
import com.ksa.financing.notification.domain.model.TemplateRoutingRule;
import com.ksa.financing.notification.domain.repository.TemplateRoutingRuleRepository;
import com.ksa.financing.notification.infrastructure.external.NovuClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationOrchestrator {

    private final TemplateRoutingRuleRepository routingRuleRepository;
    private final NotificationPreferenceService preferenceService;
    private final NovuClient novuClient;

    @org.springframework.beans.factory.annotation.Value("${novu.admin-subscriber-id:admin_global}")
    private String adminSubscriberId;

    private static final Set<String> ADMIN_EVENTS = Set.of(
            "FRAUD_CHECK_FAILED",
            "SUSPICIOUS_ACTIVITY",
            "FRAUD_DETECTED",
            "FRAUD_ALERT",
            "BLACKLIST_ADDED",
            "LOAN_APPROVED",
            "LOAN_DISBURSED",
            "LOAN_RESCHEDULED",
            "PAYMENT_FAILED",
            "PAYMENT_OVERDUE",
            "LARGE_PAYMENT",
            "MANUAL_APPROVAL_REQUIRED",
            "APPROVAL_SLA_BREACHED",
            "PAYMENT_DUE"
    );

    public void processEvent(String eventType, Map<String, Object> payload) {
        UUID tenantId = extractUuid(payload, "tenantId");
        UUID customerId = extractUuid(payload, "customerId");

        if (tenantId == null) {
            log.warn("Missing tenantId in event {}. Skipping notification.", eventType);
            return;
        }

        log.info("Orchestrating notification for event: {} tenant: {} customer: {}", eventType, tenantId, customerId);

        // 1. Find Routing Rules for this event
        List<TemplateRoutingRule> rules = routingRuleRepository.findByTenantIdAndEventTypeAndActiveTrue(tenantId, eventType);
        
        if (rules.isEmpty()) {
            log.debug("No active routing rules found for event: {}", eventType);
            return;
        }

        // Customer preferences — auto-creates with default language ('en') if missing
        NotificationPreference prefs = null;
        if (customerId != null) {
            prefs = preferenceService.getOrCreateDefault(tenantId, customerId);
        }

        // Dispatch to Novu — each rule decides its own target by rule_code prefix
        for (TemplateRoutingRule rule : rules) {
            boolean isAdminRule = rule.getRuleCode() != null && rule.getRuleCode().startsWith("ADMIN_");
            String targetSubscriberId = isAdminRule
                    ? adminSubscriberId
                    : (customerId != null ? customerId.toString() : null);

            if (targetSubscriberId == null) {
                log.warn("No subscriber resolved for rule {}. Skipping.", rule.getRuleCode());
                continue;
            }

            // Special case: PAYMENT_DUE admin notification only when actually due (daysUntilDue <= 0)
            if (isAdminRule && "PAYMENT_DUE".equals(eventType)) {
                Integer daysUntilDue = extractInt(payload, "daysUntilDue");
                if (daysUntilDue != null && daysUntilDue > 0) {
                    log.info("Skipping admin PAYMENT_DUE — daysUntilDue={} (will trigger on due date)", daysUntilDue);
                    continue;
                }
            }

            boolean shouldSend = isAdminRule || isChannelEnabled(rule.getChannel(), prefs);
            if (!shouldSend) {
                log.info("Channel {} disabled for customer {}. Skipping rule {}.",
                        rule.getChannel(), customerId, rule.getRuleCode());
                continue;
            }

            novuClient.triggerEvent(
                    rule.getNovuTemplateId(),
                    targetSubscriberId,
                    payload,
                    prefs != null ? prefs.getPreferredLanguage() : NotificationPreferenceService.DEFAULT_LANGUAGE
            );
        }
    }

    private Integer extractInt(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isChannelEnabled(TemplateRoutingRule.NotificationChannel channel, NotificationPreference prefs) {
        return switch (channel) {
            case SMS -> prefs.isSmsEnabled();
            case EMAIL -> prefs.isEmailEnabled();
            case PUSH -> prefs.isPushEnabled();
            case WHATSAPP, IN_APP -> true; // Default enabled for now
        };
    }

    private UUID extractUuid(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) return null;
        try {
            return UUID.fromString(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
