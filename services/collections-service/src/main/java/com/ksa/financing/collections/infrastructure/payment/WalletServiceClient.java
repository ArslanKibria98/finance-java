package com.ksa.financing.collections.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for wallet-service operations used by collections payment flow.
 *
 * - {@link #getAvailableBalance(UUID, UUID, String)} → JWT-protected /api/v1/wallets/by-customer/{id}
 * - {@link #debitForLoan}                            → internal /internal/wallets/debit-for-loan
 *
 * Returns the standard wallet error codes (WALLET.BALANCE.INSUFFICIENT, WALLET.STATUS.NOT_ACTIVE, etc.)
 * by surfacing the upstream response unchanged.
 */
@Slf4j
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String walletServiceUrl;

    public WalletServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.wallet-service-url:${WALLET_SERVICE_URL:http://wallet-service:8088}}") String walletServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.walletServiceUrl = walletServiceUrl;
    }

    /**
     * Read the customer's available wallet balance.
     * Uses the caller's JWT (forwarded) so wallet-service's @SecuredEndpoint passes.
     */
    public BigDecimal getAvailableBalance(UUID tenantId, UUID customerId, String bearerToken) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            headers.set("X-Tenant-Id", tenantId.toString());

            var url = walletServiceUrl + "/api/v1/wallets/by-customer/" + customerId;
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Wallet lookup returned non-2xx for customer: " + customerId);
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode availableNode = root.path("availableBalance");
            if (availableNode.isMissingNode() || availableNode.isNull()) {
                throw new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Wallet response missing availableBalance for customer: " + customerId);
            }
            return new BigDecimal(availableNode.asText());
        } catch (HttpStatusCodeException e) {
            log.warn("Wallet balance lookup failed: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                throw new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Wallet not found for customer: " + customerId);
            }
            throw new BusinessException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Wallet balance lookup failed: " + e.getStatusCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Wallet balance lookup error: {}", e.getMessage(), e);
            throw new BusinessException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Wallet balance lookup error: " + e.getMessage());
        }
    }
    /**
     * Read the authenticated caller's wallet via /api/v1/wallets/me/balance.
     * That endpoint resolves the Keycloak {@code sub} → customer-service {@code customer.id}
     * internally (JWT subject is NOT the same UUID as the wallet's customerId).
     * Returns both the resolved customerId and availableBalance.
     */
    public WalletSnapshot getMyWallet(UUID tenantId, String bearerToken) {
        try {
            var headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken);
            headers.set("X-Tenant-Id", tenantId.toString());

            var url = walletServiceUrl + "/api/v1/wallets/me/balance";
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Wallet lookup returned non-2xx");
            }

            // Response is wrapped: { "data": { ...wallet... }, "message": "success" }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode body = root.has("data") ? root.path("data") : root;
            JsonNode customerNode = body.path("customerId");
            JsonNode availableNode = body.path("availableBalance");
            if (customerNode.isMissingNode() || customerNode.isNull()
                    || availableNode.isMissingNode() || availableNode.isNull()) {
                throw new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Wallet response missing customerId/availableBalance");
            }
            return new WalletSnapshot(
                    UUID.fromString(customerNode.asText()),
                    new BigDecimal(availableNode.asText()));
        } catch (HttpStatusCodeException e) {
            log.warn("Wallet balance lookup failed: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                throw new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Wallet not found for authenticated customer");
            }
            throw new BusinessException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Wallet balance lookup failed: " + e.getStatusCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Wallet balance lookup error: {}", e.getMessage(), e);
            throw new BusinessException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Wallet balance lookup error: " + e.getMessage());
        }
    }

    public record WalletSnapshot(UUID customerId, BigDecimal availableBalance) {}

    /**
     * Debit the customer's wallet via the internal endpoint.
     * Wallet-service performs its own balance & status checks and returns
     * 422 WALLET.BALANCE.INSUFFICIENT if funds are insufficient — surfaced unchanged.
     */
    public DebitResult debitForLoan(
            UUID tenantId,
            UUID customerId,
            UUID loanId,
            BigDecimal amount,
            String description,
            String idempotencyKey) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId.toString());
            headers.set("X-Caller-Service", "collections-service");

            var body = Map.of(
                    "customerId", customerId.toString(),
                    "amount", amount,
                    "purpose", "INSTALLMENT_PAYMENT",
                    "referenceType", "LOAN_REPAYMENT",
                    "referenceId", loanId.toString(),
                    "description", description != null ? description : "",
                    "idempotencyKey", idempotencyKey
            );

            var url = walletServiceUrl + "/internal/wallets/debit-for-loan";
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(
                        ErrorCodes.TECHNICAL_ERROR,
                        "Wallet debit returned non-2xx: " + response.getStatusCode());
            }
            // Response may be wrapped: { "data": { ...debit result... } }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode debit = root.has("data") ? root.path("data") : root;
            return new DebitResult(
                    UUID.fromString(debit.path("walletId").asText()),
                    debit.path("movementId").isNull() ? null : UUID.fromString(debit.path("movementId").asText()),
                    debit.path("fineractTransactionId").isNull() ? null : debit.path("fineractTransactionId").asLong(),
                    new BigDecimal(debit.path("newAvailableBalance").asText("0")),
                    debit.path("status").asText("COMPLETED")
            );
        } catch (HttpStatusCodeException e) {
            // Surface wallet-service business errors (e.g. INSUFFICIENT_FUNDS) by parsing
            // the standardized error body and re-throwing as BusinessException so the
            // GlobalExceptionHandler maps it to the same status the caller would receive.
            log.warn("Wallet debit rejected: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            try {
                JsonNode err = objectMapper.readTree(e.getResponseBodyAsString());
                String code = err.path("code").asText(ErrorCodes.TECHNICAL_ERROR);
                String message = err.path("message").asText("Wallet debit failed");
                throw new BusinessException(code, message);
            } catch (BusinessException be) {
                throw be;
            } catch (Exception parseEx) {
                throw new BusinessException(
                        ErrorCodes.TECHNICAL_ERROR,
                        "Wallet debit failed: " + e.getStatusCode());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Wallet debit error: {}", e.getMessage(), e);
            throw new BusinessException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Wallet debit error: " + e.getMessage());
        }
    }

    public record DebitResult(
            UUID walletId,
            UUID movementId,
            Long fineractTransactionId,
            BigDecimal newAvailableBalance,
            String status
    ) {}
}
