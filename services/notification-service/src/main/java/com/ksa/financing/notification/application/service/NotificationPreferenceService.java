package com.ksa.financing.notification.application.service;

import com.ksa.financing.notification.domain.model.NotificationPreference;
import com.ksa.financing.notification.domain.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationPreferenceService {

    public static final String DEFAULT_LANGUAGE = "en";
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "ar", "ur", "es");

    private final NotificationPreferenceRepository repository;

    @Transactional
    public NotificationPreference getOrCreateDefault(UUID tenantId, UUID customerId) {
        return repository.findFirstByTenantIdAndCustomerIdOrderByCreatedAtAsc(tenantId, customerId)
                .orElseGet(() -> createDefault(tenantId, customerId, DEFAULT_LANGUAGE));
    }

    @Transactional
    public NotificationPreference createDefault(UUID tenantId, UUID customerId, String preferredLanguage) {
        String language = normalizeLanguage(preferredLanguage);

        return repository.findFirstByTenantIdAndCustomerIdOrderByCreatedAtAsc(tenantId, customerId)
                .orElseGet(() -> {
                    NotificationPreference prefs = NotificationPreference.builder()
                            .tenantId(tenantId)
                            .customerId(customerId)
                            .preferredLanguage(language)
                            .smsEnabled(true)
                            .emailEnabled(true)
                            .pushEnabled(true)
                            .build();
                    try {
                        NotificationPreference saved = repository.save(prefs);
                        log.info("Created default notification preference: tenantId={} customerId={} language={}",
                                tenantId, customerId, language);
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        // Concurrent thread won the race and inserted the GLOBAL row first
                        // (uq_prefs_global_customer). Re-read the winner instead of failing.
                        log.debug("Concurrent default-preference insert for customerId={} — reusing existing row", customerId);
                        return repository.findFirstByTenantIdAndCustomerIdOrderByCreatedAtAsc(tenantId, customerId)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Transactional
    public NotificationPreference updatePreference(UUID tenantId, UUID customerId,
                                                   String preferredLanguage,
                                                   Boolean smsEnabled,
                                                   Boolean emailEnabled,
                                                   Boolean pushEnabled) {
        NotificationPreference prefs = repository.findFirstByTenantIdAndCustomerIdOrderByCreatedAtAsc(tenantId, customerId)
                .orElseGet(() -> createDefault(tenantId, customerId, DEFAULT_LANGUAGE));

        if (preferredLanguage != null) {
            prefs.setPreferredLanguage(normalizeLanguage(preferredLanguage));
        }
        if (smsEnabled != null) {
            prefs.setSmsEnabled(smsEnabled);
        }
        if (emailEnabled != null) {
            prefs.setEmailEnabled(emailEnabled);
        }
        if (pushEnabled != null) {
            prefs.setPushEnabled(pushEnabled);
        }

        NotificationPreference saved = repository.save(prefs);
        log.info("Updated notification preference: tenantId={} customerId={} language={}",
                tenantId, customerId, saved.getPreferredLanguage());
        return saved;
    }

    /**
     * Persists the customer's display currency (synced from customer-service profile events).
     * No-op when the currency is blank or unchanged, to avoid redundant writes.
     */
    @Transactional
    public void setCustomerCurrency(UUID tenantId, UUID customerId, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return;
        }
        String normalized = currencyCode.trim().toUpperCase();
        NotificationPreference prefs = getOrCreateDefault(tenantId, customerId);
        if (normalized.equals(prefs.getCurrencyCode())) {
            return;
        }
        prefs.setCurrencyCode(normalized);
        repository.save(prefs);
        log.info("Set notification currency: tenantId={} customerId={} currency={}",
                tenantId, customerId, normalized);
    }

    private String normalizeLanguage(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = code.toLowerCase().trim();
        if (!SUPPORTED_LANGUAGES.contains(normalized)) {
            log.warn("Unsupported language '{}' — falling back to default '{}'", code, DEFAULT_LANGUAGE);
            return DEFAULT_LANGUAGE;
        }
        return normalized;
    }
}
