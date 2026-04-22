package com.ksa.financing.collections.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.collections.domain.port.out.HyperPayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapter that calls middleware-third-party to execute HyperPay API calls.
 *
 * Flow:
 *  1. POST {{middlewareUrl}}/api/v1/execute/HYPERPAY_CHECKOUT  → gets checkoutId
 *  2. POST {{middlewareUrl}}/api/v1/execute/HYPERPAY_STATUS    → verifies payment
 *  3. POST {{middlewareUrl}}/api/v1/execute/HYPERPAY_REFUND    → refunds
 *
 * Auth: X-Secret-Key header (no JWT — middleware uses secret key auth)
 */
@Component
@Slf4j
public class HyperPayMiddlewareAdapter implements HyperPayPort {

    private static final String API_CODE_CHECKOUT     = "HYPERPAY_CHECKOUT";
    private static final String API_CODE_STATUS       = "HYPERPAY_STATUS";
    private static final String API_CODE_REFUND       = "HYPERPAY_REFUND";
    private static final String API_CODE_REGISTRATION = "HYPERPAY_REGISTRATION";

    // Success result codes per HyperPay spec
    private static final String CODE_SUCCESS_SYNC  = "000.100.110";
    private static final String CODE_SUCCESS_ASYNC = "000.200.100";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${middleware.secret-key:test-mock-secret-key-2026}")
    private String secretKey;

    @Value("${middleware.caller-service:collections-service}")
    private String callerService;

    public HyperPayMiddlewareAdapter(
            @Value("${middleware.base-url:http://middleware-third-party:8093}") String middlewareBaseUrl,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(middlewareBaseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public HyperPayCheckoutResult createCheckout(BigDecimal amount, String currency,
                                                  String paymentBrand, String idempotencyKey) {
        log.info("HyperPay checkout: amount={} currency={} brand={}", amount, currency, paymentBrand);

        String requestBody = """
                {
                    "amount": %s,
                    "currency": "%s",
                    "paymentType": "DB",
                    "paymentBrand": "%s",
                    "descriptor": "KSA Financing Payment"
                }
                """.formatted(amount.toPlainString(), currency, paymentBrand != null ? paymentBrand : "MADA");

        try {
            var response = executeMiddlewareApi(API_CODE_CHECKOUT, requestBody, idempotencyKey);

            if (!response.success()) {
                log.warn("HyperPay checkout failed: {}", response.errorMessage());
                return new HyperPayCheckoutResult(false, null, null, response.errorMessage(), response.rawBody());
            }

            var body = parseJson(response.rawBody());
            String checkoutId = body.path("id").asText(null);
            String resultCode = body.path("result").path("code").asText(null);
            String resultDesc = body.path("result").path("description").asText(null);

            boolean success = checkoutId != null && isSuccessCode(resultCode);
            log.info("HyperPay checkout {}: checkoutId={}", success ? "succeeded" : "failed", checkoutId);

            return new HyperPayCheckoutResult(success, checkoutId, resultCode, resultDesc, response.rawBody());

        } catch (Exception e) {
            log.error("HyperPay checkout error: {}", e.getMessage(), e);
            return new HyperPayCheckoutResult(false, null, null, e.getMessage(), null);
        }
    }

    @Override
    public HyperPayStatusResult getPaymentStatus(String checkoutId) {
        log.info("HyperPay status check: checkoutId={}", checkoutId);

        String requestBody = """
                {
                    "checkoutId": "%s"
                }
                """.formatted(checkoutId);

        try {
            var response = executeMiddlewareApi(API_CODE_STATUS, requestBody, null);

            if (!response.success()) {
                log.warn("HyperPay status check failed: {}", response.errorMessage());
                return new HyperPayStatusResult(false, false, null, null,
                        response.errorMessage(), null, null, null, null, response.rawBody());
            }

            var body = parseJson(response.rawBody());
            String resultCode = body.path("result").path("code").asText(null);
            String resultDesc = body.path("result").path("description").asText(null);
            String paymentId = body.path("id").asText(null);
            String paymentBrand = body.path("paymentBrand").asText(null);
            String amount = body.path("amount").asText(null);
            String currency = body.path("currency").asText(null);
            String merchantTxnId = body.path("merchantTransactionId").asText(null);

            boolean paymentSuccessful = isSuccessCode(resultCode);
            log.info("HyperPay payment status: code={} successful={}", resultCode, paymentSuccessful);

            return new HyperPayStatusResult(true, paymentSuccessful, paymentId, resultCode,
                    resultDesc, paymentBrand, amount, currency, merchantTxnId, response.rawBody());

        } catch (Exception e) {
            log.error("HyperPay status error: {}", e.getMessage(), e);
            return new HyperPayStatusResult(false, false, null, null, e.getMessage(),
                    null, null, null, null, null);
        }
    }

    @Override
    public HyperPayRefundResult refundPayment(String paymentId, BigDecimal amount, String currency) {
        log.info("HyperPay refund: paymentId={} amount={}", paymentId, amount);

        String requestBody = """
                {
                    "paymentId": "%s",
                    "amount": %s,
                    "currency": "%s"
                }
                """.formatted(paymentId, amount.toPlainString(), currency);

        try {
            var response = executeMiddlewareApi(API_CODE_REFUND, requestBody, null);

            if (!response.success()) {
                log.warn("HyperPay refund failed: {}", response.errorMessage());
                return new HyperPayRefundResult(false, null, null, response.errorMessage(), response.rawBody());
            }

            var body = parseJson(response.rawBody());
            String refundId = body.path("id").asText(null);
            String resultCode = body.path("result").path("code").asText(null);
            String resultDesc = body.path("result").path("description").asText(null);

            boolean success = isSuccessCode(resultCode);
            log.info("HyperPay refund {}: refundId={}", success ? "succeeded" : "failed", refundId);

            return new HyperPayRefundResult(success, refundId, resultCode, resultDesc, response.rawBody());

        } catch (Exception e) {
            log.error("HyperPay refund error: {}", e.getMessage(), e);
            return new HyperPayRefundResult(false, null, null, e.getMessage(), null);
        }
    }

    // ==================== INTERNAL ====================

    private MiddlewareResponse executeMiddlewareApi(String apiCode, String requestBody, String idempotencyKey) {
        try {
            var response = restClient.post()
                    .uri("/api/v1/execute/{apiCode}", apiCode)
                    .header("Content-Type", "application/json")
                    .header("X-Secret-Key", secretKey)
                    .header("X-Caller-Service", callerService)
                    .header("X-Idempotency-Key", idempotencyKey != null ? idempotencyKey : "")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // Parse middleware wrapper response
            var parsed = parseJson(response);
            boolean success = parsed.path("success").asBoolean(false);
            String rawBody = parsed.path("responseBody").toString();
            String errorMessage = parsed.path("errorMessage").asText(null);

            return new MiddlewareResponse(success, rawBody, errorMessage);

        } catch (Exception e) {
            log.error("Middleware call failed for {}: {}", apiCode, e.getMessage());
            return new MiddlewareResponse(false, null, e.getMessage());
        }
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json != null ? json : "{}");
        } catch (Exception e) {
            log.warn("Failed to parse JSON response: {}", e.getMessage());
            try {
                return objectMapper.createObjectNode();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * HyperPay success codes per their result code matrix.
     * Codes starting with "000.1" or "000.2" are success/pending.
     */
    private boolean isSuccessCode(String code) {
        if (code == null) return false;
        return code.startsWith("000.1") || code.startsWith("000.2");
    }

    private record MiddlewareResponse(boolean success, String rawBody, String errorMessage) {}
}
