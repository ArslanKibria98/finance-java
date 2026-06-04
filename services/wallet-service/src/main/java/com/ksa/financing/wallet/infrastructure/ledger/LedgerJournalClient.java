package com.ksa.financing.wallet.infrastructure.ledger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.wallet.domain.port.out.LedgerPostingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Posts GL journal entries to ledger-service for external fund transfers.
 *
 * Mirrors lending-service's LedgerServiceClient: calls the trusted internal endpoint
 * {@code POST /internal/v1/journal-entries} with X-Tenant-Id / X-Caller-Service headers.
 *
 * Best-effort: on any failure we log and return null so the transfer still completes;
 * the GL entry can be reconciled later.
 */
@Slf4j
@Component
public class LedgerJournalClient implements LedgerPostingPort {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ledgerServiceUrl;
    private final String consumerWalletAccount;
    private final String rtpClearingAccount;
    private final String ibftClearingAccount;
    private final String systemUserId;

    public LedgerJournalClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${ksa.ledger.client.base-url:http://ledger-service:8095}") String ledgerServiceUrl,
            @Value("${ksa.wallet.gl.accounts.consumer-wallet:110401}") String consumerWalletAccount,
            @Value("${ksa.wallet.gl.accounts.rtp-clearing:120601}") String rtpClearingAccount,
            @Value("${ksa.wallet.gl.accounts.ibft-clearing:120602}") String ibftClearingAccount,
            @Value("${ksa.wallet.gl.system-user-id:00000000-0000-0000-0000-000000000000}") String systemUserId) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.ledgerServiceUrl = ledgerServiceUrl;
        this.consumerWalletAccount = consumerWalletAccount;
        this.rtpClearingAccount = rtpClearingAccount;
        this.ibftClearingAccount = ibftClearingAccount;
        this.systemUserId = systemUserId;
    }

    @Override
    public String postIbft(UUID tenantId, UUID ibftId, String ibftNumber, Direction direction,
                           BigDecimal amount, String idempotencyKey, UUID createdBy) {
        String debitAccount = direction == Direction.OUTBOUND ? consumerWalletAccount : ibftClearingAccount;
        String creditAccount = direction == Direction.OUTBOUND ? ibftClearingAccount : consumerWalletAccount;
        return postEntry(tenantId, "IBFT", ibftId, "IBFT_" + direction,
                "IBFT " + direction + " " + ibftNumber, debitAccount, creditAccount,
                amount, idempotencyKey, createdBy, ibftNumber);
    }

    private String postEntry(UUID tenantId, String referenceType, UUID referenceId, String transactionType,
                             String description, String debitAccount, String creditAccount, BigDecimal amount,
                             String idempotencyKey, UUID createdBy, String label) {
        try {
            var lines = List.of(
                    buildLine(debitAccount, amount, BigDecimal.ZERO, description),
                    buildLine(creditAccount, BigDecimal.ZERO, amount, description));
            var request = Map.of(
                    "entryDate", LocalDate.now().toString(),
                    "referenceType", referenceType,
                    "referenceId", referenceId.toString(),
                    "transactionType", transactionType,
                    "description", description,
                    "lines", lines,
                    "idempotencyKey", idempotencyKey);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());
            headers.set("X-Caller-Service", "wallet-service");
            headers.set("X-Created-By", createdBy != null ? createdBy.toString() : systemUserId);
            var response = restTemplate.exchange(ledgerServiceUrl + "/internal/v1/journal-entries",
                    HttpMethod.POST, new HttpEntity<>(objectMapper.writeValueAsString(request), headers), String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                JsonNode entity = node.has("data") ? node.path("data") : node;
                String entryId = entity.path("id").asText(null);
                log.info("GL {} entry posted for {} → entryId={}", referenceType, label, entryId);
                return entryId;
            }
            log.error("Ledger-service non-2xx for {} {}: status={}", referenceType, label, response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Failed to post GL {} entry for {} (continuing): {}", referenceType, label, e.getMessage());
            return null;
        }
    }

    @Override
    public String postExternalTransfer(UUID tenantId,
                                       UUID transferId,
                                       String transferNumber,
                                       Direction direction,
                                       BigDecimal amount,
                                       String idempotencyKey,
                                       UUID createdBy) {
        try {
            // OUTBOUND: funds leave the customer wallet into the Scotia clearing account.
            //   Dr Consumer Wallet / Cr Scotia RTP Clearing
            // INBOUND: funds arrive from the Scotia clearing account into the customer wallet.
            //   Dr Scotia RTP Clearing / Cr Consumer Wallet
            String debitAccount = direction == Direction.OUTBOUND ? consumerWalletAccount : rtpClearingAccount;
            String creditAccount = direction == Direction.OUTBOUND ? rtpClearingAccount : consumerWalletAccount;

            var lines = List.of(
                    buildLine(debitAccount, amount, BigDecimal.ZERO,
                            "External transfer " + direction + " — " + transferNumber),
                    buildLine(creditAccount, BigDecimal.ZERO, amount,
                            "External transfer " + direction + " — " + transferNumber)
            );

            var request = Map.of(
                    "entryDate", LocalDate.now().toString(),
                    "referenceType", "EXTERNAL_TRANSFER",
                    "referenceId", transferId.toString(),
                    "transactionType", "EXTERNAL_TRANSFER_" + direction,
                    "description", "External fund transfer " + direction + " " + transferNumber,
                    "lines", lines,
                    "idempotencyKey", idempotencyKey
            );

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());
            headers.set("X-Caller-Service", "wallet-service");
            // ledger-service requires a creator; fall back to the system user for system-initiated legs (e.g. inbound).
            headers.set("X-Created-By", createdBy != null ? createdBy.toString() : systemUserId);

            var url = ledgerServiceUrl + "/internal/v1/journal-entries";
            var body = objectMapper.writeValueAsString(request);
            var response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode node = objectMapper.readTree(response.getBody());
                // ledger-service may wrap the entity in a {data:{...}} envelope
                JsonNode entity = node.has("data") ? node.path("data") : node;
                String entryId = entity.path("id").asText(null);
                log.info("GL entry posted for external transfer {} → entryId={}", transferNumber, entryId);
                return entryId;
            }
            log.error("Ledger-service returned non-2xx for external transfer {}: status={}",
                    transferNumber, response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Failed to post GL entry for external transfer {} (continuing): {}",
                    transferNumber, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> buildLine(String accountCode, BigDecimal debit, BigDecimal credit, String description) {
        return Map.of(
                "accountCode", accountCode,
                "debitAmount", debit,
                "creditAmount", credit,
                "description", description
        );
    }
}
