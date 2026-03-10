package com.ksa.financing.customer.infrastructure.http;

import com.ksa.financing.customer.domain.port.out.PiiVaultPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock PII Vault adapter for development and testing.
 * Stores PII data in-memory using a ConcurrentHashMap.
 * Active when pii.vault.mock=true.
 */
@Component
@ConditionalOnProperty(name = "pii.vault.mock", havingValue = "true")
@Slf4j
public class MockPiiVaultAdapter implements PiiVaultPort {

    private final ConcurrentHashMap<UUID, Map<String, String>> piiStore = new ConcurrentHashMap<>();

    @Override
    public UUID storePii(UUID globalUid, Map<String, String> piiFields) {
        log.info("[MOCK] Storing PII for globalUid: {} with {} fields", globalUid, piiFields.size());
        piiStore.put(globalUid, new ConcurrentHashMap<>(piiFields));
        log.debug("[MOCK] PII fields stored: {}", piiFields.keySet());
        return globalUid;
    }

    @Override
    public Map<String, String> retrievePii(UUID globalUid, String accessToken) {
        log.info("[MOCK] Retrieving PII for globalUid: {}", globalUid);
        Map<String, String> pii = piiStore.get(globalUid);
        if (pii != null) {
            log.debug("[MOCK] PII found with {} fields", pii.size());
            return Collections.unmodifiableMap(pii);
        }
        log.warn("[MOCK] No PII found for globalUid: {}", globalUid);
        return Collections.emptyMap();
    }

    @Override
    public void deletePii(UUID globalUid) {
        log.info("[MOCK] Deleting PII for globalUid: {}", globalUid);
        Map<String, String> removed = piiStore.remove(globalUid);
        if (removed != null) {
            log.info("[MOCK] PII deleted successfully for globalUid: {} ({} fields removed)", globalUid, removed.size());
        } else {
            log.warn("[MOCK] No PII found to delete for globalUid: {}", globalUid);
        }
    }
}
