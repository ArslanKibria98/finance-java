package com.ksa.financing.middleware.infrastructure.settlement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.middleware.domain.port.in.ExecuteApiUseCase.ExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * After a successful payment-rail call (e.g. Scotia RTP commit), mirrors the money movement
 * into our wallets by calling wallet-service /internal/wallets/settle with the caller's
 * debtorAccount / creditorAccount / amount. Best-effort + config-gated — never blocks or
 * fails the original execute() response.
 */
@Slf4j
@Component
public class WalletSettlementClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final boolean validateBeforeRail;
    private final Set<String> apiCodes;
    private final String walletServiceUrl;
    private final String defaultCurrency;

    public WalletSettlementClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.middleware.wallet-settlement.enabled:true}") boolean enabled,
            @Value("${app.middleware.wallet-settlement.validate-before-rail:true}") boolean validateBeforeRail,
            @Value("${app.middleware.wallet-settlement.api-codes:SCOTIABANK_PAYMENT_COMMIT}") String apiCodes,
            @Value("${app.middleware.wallet-settlement.wallet-service-url:http://wallet-service:8088}") String walletServiceUrl,
            @Value("${app.middleware.wallet-settlement.default-currency:CAD}") String defaultCurrency) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.validateBeforeRail = validateBeforeRail;
        this.apiCodes = Arrays.stream(apiCodes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        this.walletServiceUrl = walletServiceUrl;
        this.defaultCurrency = defaultCurrency;
    }

    /**
     * BEFORE the payment rail: validate the wallet debit (debtor present + funded). If invalid,
     * throws a BusinessException so the middleware returns the error to the caller and never calls
     * Scotia — so insufficient/missing-debtor is surfaced WITHOUT moving any money.
     */
    public void validateBeforeRail(String apiCode, String callerBody, java.util.UUID tenantId) {
        if (!enabled || !validateBeforeRail || apiCode == null || !apiCodes.contains(apiCode)) return;
        JsonNode body;
        try {
            body = (callerBody == null || callerBody.isBlank())
                    ? objectMapper.createObjectNode() : objectMapper.readTree(callerBody);
        } catch (Exception e) {
            return; // can't parse — let the call proceed; post-settlement will guard
        }
        String debtorAccount = text(body, "debtorAccount");
        var payload = new LinkedHashMap<String, Object>();
        payload.put("debtorAccount", debtorAccount);
        // No explicit debtor account → debtor is the authenticated user (JWT mobile_number).
        payload.put("debtorMobile", (debtorAccount == null || debtorAccount.isBlank()) ? currentUserMobile() : null);
        payload.put("creditorAccount", text(body, "creditorAccount"));
        payload.put("amount", body.get("amount"));
        payload.put("currency", text(body, "currency") != null ? text(body, "currency") : defaultCurrency);
        payload.put("reference", "validate");
        payload.put("idempotencyKey", "validate");

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-Id", tenantId.toString());
        try {
            restTemplate.exchange(walletServiceUrl + "/internal/wallets/settle/validate",
                    HttpMethod.POST, new HttpEntity<>(objectMapper.writeValueAsString(payload), headers), String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // wallet-service rejected (e.g. insufficient / missing debtor) — surface it to the caller.
            String code = "WALLET.SETTLE.REJECTED", message = "Settlement validation failed";
            try {
                JsonNode err = objectMapper.readTree(e.getResponseBodyAsString());
                if (err.hasNonNull("code")) code = err.get("code").asText();
                if (err.hasNonNull("message")) message = err.get("message").asText();
            } catch (Exception ignore) { }
            log.warn("Settlement pre-validation rejected for {}: {} - {}", apiCode, code, message);
            throw new com.ksa.financing.infra.exception.BusinessException(code, message);
        } catch (Exception e) {
            // wallet-service unreachable — fail closed for a money movement.
            log.error("Settlement pre-validation unavailable for {}: {}", apiCode, e.getMessage());
            throw new com.ksa.financing.infra.exception.BusinessException(
                    "WALLET.SETTLE.VALIDATION_UNAVAILABLE", "Wallet validation unavailable, payment not attempted");
        }
    }

    /**
     * @param apiCode      the executed API code
     * @param callerBody   the caller's ORIGINAL flat body (before template expansion)
     * @param tenantId     the resolved tenant
     * @param result       the execution result (only acted upon when success)
     */
    public void settleIfApplicable(String apiCode, String callerBody, UUID tenantId, ExecutionResult result) {
        if (!enabled || apiCode == null || !apiCodes.contains(apiCode)) return;
        if (result == null || !result.success()) return;
        try {
            JsonNode body = (callerBody == null || callerBody.isBlank())
                    ? objectMapper.createObjectNode() : objectMapper.readTree(callerBody);
            String debtorAccount = text(body, "debtorAccount");
            String debtorMobile = (debtorAccount == null || debtorAccount.isBlank()) ? currentUserMobile() : null;
            String creditorAccount = text(body, "creditorAccount");
            JsonNode amount = body.get("amount");
            if (amount == null || amount.isNull()
                    || ((debtorAccount == null || debtorAccount.isBlank()) && debtorMobile == null && creditorAccount == null)) {
                log.warn("Wallet settlement skipped for {} — missing amount/accounts in caller body", apiCode);
                return;
            }
            String currency = text(body, "currency");

            var payload = new LinkedHashMap<String, Object>();
            payload.put("debtorAccount", debtorAccount);
            payload.put("debtorMobile", debtorMobile);
            payload.put("creditorAccount", creditorAccount);
            payload.put("amount", amount);
            payload.put("currency", currency != null ? currency : defaultCurrency);
            payload.put("reference", apiCode + ":" + result.requestId());
            payload.put("idempotencyKey", result.requestId());

            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());

            var url = walletServiceUrl + "/internal/wallets/settle";
            var resp = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(payload), headers), String.class);
            log.info("Wallet settlement OK for {} (requestId={}): status={}",
                    apiCode, result.requestId(), resp.getStatusCode());
        } catch (Exception e) {
            log.error("Wallet settlement FAILED for {} (requestId={}) — continuing: {}",
                    apiCode, result.requestId(), e.getMessage(), e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText() : null;
    }

    /** The authenticated caller's mobile_number claim (when the execute endpoint carried a JWT). */
    private String currentUserMobile() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                return jwt.getClaimAsString("mobile_number");
            }
        } catch (Exception ignore) {
            // no security context (e.g. /simple endpoint) — no mobile
        }
        return null;
    }
}
