package com.ksa.financing.notification.application.service;

import com.ksa.financing.notification.domain.model.NotificationPreference;
import com.ksa.financing.notification.domain.model.TemplateRoutingRule;
import com.ksa.financing.notification.domain.repository.NotificationPreferenceRepository;
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
    private final NotificationPreferenceRepository preferenceRepository;
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
            "LOAN_RESCHEDULED"
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

        // 2. Determine if this is an Admin Event
        boolean isAdminEvent = ADMIN_EVENTS.contains(eventType);
        
        String targetSubscriberId = isAdminEvent ? adminSubscriberId : (customerId != null ? customerId.toString() : null);

        if (targetSubscriberId == null) {
            log.warn("Could not determine subscriber for event {}. Skipping.", eventType);
            return;
        }

        // 3. Get Preferences (Only for Customers, Admin gets everything)
        NotificationPreference prefs = null;
        if (!isAdminEvent && customerId != null) {
            prefs = preferenceRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                    .orElse(NotificationPreference.builder()
                            .tenantId(tenantId)
                            .customerId(customerId)
                            .preferredLanguage("ar")
                            .smsEnabled(true)
                            .emailEnabled(true)
                            .pushEnabled(true)
                            .build());
        }

        // 4. Dispatch to Novu for each matching rule
        for (TemplateRoutingRule rule : rules) {
            boolean shouldSend = isAdminEvent || isChannelEnabled(rule.getChannel(), prefs);
            
            if (shouldSend) {
                novuClient.triggerEvent(
                        rule.getNovuTemplateId(),
                        targetSubscriberId,
                        payload,
                        prefs != null ? prefs.getPreferredLanguage() : "en"
                );
            } else {
                log.info("Channel {} is disabled for customer {}. Skipping.", rule.getChannel(), customerId);
            }
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
