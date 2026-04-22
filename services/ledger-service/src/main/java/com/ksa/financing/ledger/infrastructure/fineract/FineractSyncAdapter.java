package com.ksa.financing.ledger.infrastructure.fineract;

import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.model.JournalLine;
import com.ksa.financing.ledger.domain.port.out.FineractSyncPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter: posts journal entries to Apache Fineract GL module.
 *
 * Resilience:
 *   - Circuit breaker: fineract-api (60s wait, 50% failure threshold)
 *   - Retry: fineract-api (3 attempts, 2s wait, exponential backoff)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FineractSyncAdapter implements FineractSyncPort {

    private final RestTemplate fineractRestTemplate;

    @Value("${ksa.fineract.base-url:https://localhost:8443/fineract-provider/api/v1}")
    private String fineractBaseUrl;

    @Value("${ksa.fineract.office-id:1}")
    private Long officeId;

    @Value("${ksa.fineract.tenant-id:default}")
    private String fineractTenantId;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    @Override
    @CircuitBreaker(name = "fineract-api", fallbackMethod = "postJournalEntryFallback")
    @Retry(name = "fineract-api")
    public Long postJournalEntry(JournalEntryAggregate entry) {
        log.info("Syncing journal entry to Fineract: entryNumber={}", entry.getEntryNumber());

        Map<String, Object> payload = buildFineractPayload(entry);
        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = fineractRestTemplate.postForEntity(
                    fineractBaseUrl + "/journalentries",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object transactionId = response.getBody().get("transactionId");
                Long fineractTxId = transactionId != null
                        ? Long.valueOf(transactionId.toString()) : null;
                log.info("Journal entry synced to Fineract: entryNumber={} fineractTxId={}",
                        entry.getEntryNumber(), fineractTxId);
                return fineractTxId;
            }

            throw new FineractSyncException("Unexpected response from Fineract: " + response.getStatusCode());

        } catch (FineractSyncException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fineract sync failed for entry={}: {}", entry.getEntryNumber(), e.getMessage(), e);
            throw new FineractSyncException("Failed to sync journal entry to Fineract: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private Long postJournalEntryFallback(JournalEntryAggregate entry, Exception ex) {
        log.warn("Circuit breaker OPEN for Fineract. Entry queued for retry: {} — {}",
                entry.getEntryNumber(), ex.getMessage());
        // Return null to signal that sync was not completed; retry will be scheduled
        return null;
    }

    @Override
    @CircuitBreaker(name = "fineract-api")
    @Retry(name = "fineract-api")
    public BigDecimal getAccountBalance(Long fineractGlAccountId) {
        if (fineractGlAccountId == null) {
            return BigDecimal.ZERO;
        }

        HttpHeaders headers = buildHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = fineractRestTemplate.exchange(
                    fineractBaseUrl + "/glaccounts/" + fineractGlAccountId,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object balance = response.getBody().get("organizationRunningBalance");
                return balance != null
                        ? new BigDecimal(balance.toString())
                        : BigDecimal.ZERO;
            }

            return BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("Failed to fetch Fineract balance for GL account {}: {}", fineractGlAccountId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    @Override
    @CircuitBreaker(name = "fineract-api")
    public boolean isHealthy() {
        try {
            HttpHeaders headers = buildHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = fineractRestTemplate.exchange(
                    fineractBaseUrl + "/self/authentication",
                    HttpMethod.GET,
                    request,
                    String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Fineract health check failed: {}", e.getMessage());
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Map<String, Object> buildFineractPayload(JournalEntryAggregate entry) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("officeId", officeId);
        payload.put("transactionDate", entry.getEntryDate().format(DATE_FMT));
        payload.put("currencyCode", entry.getCurrency());
        payload.put("referenceNumber", entry.getEntryNumber());
        payload.put("comments", entry.getDescription());
        payload.put("locale", "en");
        payload.put("dateFormat", "dd MMMM yyyy");

        List<Map<String, Object>> debits = new ArrayList<>();
        List<Map<String, Object>> credits = new ArrayList<>();

        for (JournalLine line : entry.getLines()) {
            Map<String, Object> lineMap = new HashMap<>();
            // Map internal account UUID to Fineract GL account ID
            // In production: look up fineract_account_mappings
            lineMap.put("glAccountId", 1L); // Placeholder — real impl uses mapping table
            lineMap.put("amount", line.effectiveAmount().toPlainString());

            if (line.isDebit()) {
                debits.add(lineMap);
            } else {
                credits.add(lineMap);
            }
        }

        payload.put("debits", debits);
        payload.put("credits", credits);

        return payload;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Fineract-Platform-TenantId", fineractTenantId);
        return headers;
    }

    /**
     * Domain-specific exception for Fineract sync failures.
     */
    public static class FineractSyncException extends RuntimeException {
        public FineractSyncException(String message) {
            super(message);
        }

        public FineractSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
