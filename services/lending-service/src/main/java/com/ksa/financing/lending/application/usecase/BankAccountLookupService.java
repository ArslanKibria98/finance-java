package com.ksa.financing.lending.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.adapter.rest.response.BankAccountInfoResponse;
import com.ksa.financing.lending.adapter.rest.response.BankAccountInfoResponse.BankAccountItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Looks up bank accounts for a customer in two stages:
 * 1. First checks customer-service for existing bank accounts (by customerId)
 * 2. If none found, calls Tarabut Open Banking via middleware-third-party (by NID)
 */
@Slf4j
@Service
public class BankAccountLookupService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String customerServiceUrl;
    private final String middlewareUrl;

    public BankAccountLookupService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.customer-service-url}") String customerServiceUrl,
            @Value("${app.services.middleware-url}") String middlewareUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.customerServiceUrl = customerServiceUrl;
        this.middlewareUrl = middlewareUrl;
    }

    /**
     * Look up bank accounts: customer-service first, then Tarabut fallback.
     *
     * @param customerId the customer UUID
     * @param nationalId the 10-digit Saudi National ID
     * @param tenantId   the tenant UUID
     * @return response with source and list of bank accounts
     */
    public BankAccountInfoResponse lookupBankAccounts(String customerId, String nationalId, String tenantId, String bearerToken) {
        // Resolve actual customer-service ID by NID (customerId from caller may be identity-service internalUserId)
        String resolvedCustomerId = resolveCustomerIdByNid(nationalId, bearerToken);
        String effectiveCustomerId = resolvedCustomerId != null ? resolvedCustomerId : customerId;
        if (resolvedCustomerId != null && !resolvedCustomerId.equals(customerId)) {
            log.info("Resolved customer-service ID: {} (caller sent: {})", resolvedCustomerId, customerId);
        }

        // Stage 1: Check customer-service
        List<BankAccountItem> accounts = fetchFromCustomerService(effectiveCustomerId, bearerToken);
        if (!accounts.isEmpty()) {
            log.info("Found {} bank account(s) from customer-service for customer: {}", accounts.size(), effectiveCustomerId);
            return new BankAccountInfoResponse("CUSTOMER_SERVICE", accounts);
        }

        // Stage 2: Fallback to Tarabut Open Banking via middleware
        log.info("No bank accounts in customer-service for customer: {}, fetching from Tarabut via NID", effectiveCustomerId);
        accounts = fetchFromTarabut(nationalId, tenantId, bearerToken);
        if (!accounts.isEmpty()) {
            log.info("Found {} bank account(s) from Tarabut for NID: ***{}", accounts.size(), nationalId.substring(nationalId.length() - 4));
            return new BankAccountInfoResponse("TARABUT", accounts);
        }

        log.info("No bank accounts found from any source for customer: {}", effectiveCustomerId);
        return new BankAccountInfoResponse("NONE", List.of());
    }

    /**
     * Resolve customer-service customer ID by National ID.
     * GET /api/v1/customers/by-nid/{nationalId}
     */
    private String resolveCustomerIdByNid(String nationalId, String bearerToken) {
        try {
            String url = customerServiceUrl + "/api/v1/customers/by-nid/" + nationalId;

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }

            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            var body = objectMapper.readTree(response.getBody());
            // Handle wrapped response: { "data": { "id": "..." } } or direct { "id": "..." }
            JsonNode customerNode = body.has("data") ? body.get("data") : body;
            return textOrNull(customerNode, "id");

        } catch (Exception e) {
            log.warn("Customer ID resolution by NID failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch bank accounts from customer-service REST API.
     * GET /api/v1/customers/{customerId}/bank-accounts
     */
    private List<BankAccountItem> fetchFromCustomerService(String customerId, String bearerToken) {
        try {
            String url = customerServiceUrl + "/api/v1/customers/" + customerId + "/bank-accounts";

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }

            var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            var body = objectMapper.readTree(response.getBody());

            // Handle wrapped response: { "data": [...] } or direct array
            JsonNode accountsNode = body.isArray() ? body : (body.has("data") ? body.get("data") : body);
            if (!accountsNode.isArray()) {
                return List.of();
            }

            List<BankAccountItem> accounts = new ArrayList<>();
            for (JsonNode node : accountsNode) {
                accounts.add(new BankAccountItem(
                        textOrNull(node, "bankName"),
                        textOrNull(node, "bankCode"),
                        textOrNull(node, "iban"),
                        textOrNull(node, "accountHolderName"),
                        textOrNull(node, "accountType"),
                        boolOrFalse(node, "salaryAccount") || boolOrFalse(node, "isSalaryAccount"),
                        textOrNull(node, "status")
                ));
            }
            return accounts;

        } catch (Exception e) {
            log.warn("Customer-service bank account lookup failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetch bank accounts from Tarabut Open Banking via middleware-third-party.
     * Uses TARABUT_KSA_GET_ACCOUNTS API code.
     */
    private List<BankAccountItem> fetchFromTarabut(String nationalId, String tenantId, String bearerToken) {
        try {
            String url = middlewareUrl + "/api/v1/execute/TARABUT_KSA_GET_ACCOUNTS/simple";

            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "nationalId", nationalId
            ));

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }
            headers.set("X-Idempotency-Key", "bank-lookup-" + nationalId + "-" + System.currentTimeMillis());
            headers.set("X-National-Id", nationalId);
            headers.set("X-Caller-Service", "lending-service");

            var response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            var body = objectMapper.readTree(response.getBody());

            if (!body.has("success") || !body.get("success").asBoolean()) {
                log.warn("Tarabut API returned failure: {}", textOrNull(body, "errorMessage"));
                return List.of();
            }

            // Parse Tarabut response — accounts are in responseBody
            JsonNode responseBody = body.has("responseBody")
                    ? objectMapper.readTree(body.get("responseBody").asText()) : body;

            JsonNode accountsNode = responseBody.has("accounts") ? responseBody.get("accounts") : responseBody;
            if (!accountsNode.isArray()) {
                return List.of();
            }

            List<BankAccountItem> accounts = new ArrayList<>();
            for (JsonNode node : accountsNode) {
                accounts.add(new BankAccountItem(
                        textOrNull(node, "bankName"),
                        textOrNull(node, "bankCode"),
                        textOrNull(node, "iban"),
                        textOrNull(node, "accountHolderName"),
                        textOrNull(node, "accountType"),
                        boolOrFalse(node, "salaryAccount"),
                        "PENDING_VERIFICATION"
                ));
            }
            return accounts;

        } catch (Exception e) {
            log.warn("Tarabut bank account lookup failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Sync a bank account to customer-service so it appears in the list API.
     * Called after bank account is submitted in loan application workflow.
     */
    public void syncBankAccountToCustomerService(String customerId, String bankName, String bankCode,
                                                  String iban, String accountHolderName, String bearerToken) {
        try {
            String url = customerServiceUrl + "/internal/customers/" + customerId + "/bank-accounts";
            log.info("Syncing bank account to customer-service for customer: {}", customerId);

            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "bankName", bankName != null ? bankName : "",
                    "bankCode", bankCode != null ? bankCode : "",
                    "iban", iban != null ? iban : "",
                    "accountHolderName", accountHolderName != null ? accountHolderName : "",
                    "accountType", "CURRENT",
                    "isPrimary", true,
                    "isSalaryAccount", false
            ));

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            }

            var response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);
            log.info("Bank account synced to customer-service: status={}", response.getStatusCode());

        } catch (Exception e) {
            log.warn("Failed to sync bank account to customer-service (non-blocking): {}", e.getMessage());
        }
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private boolean boolOrFalse(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() && node.get(field).asBoolean();
    }
}
