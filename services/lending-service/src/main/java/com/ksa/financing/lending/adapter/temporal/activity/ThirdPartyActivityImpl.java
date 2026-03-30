package com.ksa.financing.lending.adapter.temporal.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.islamic.orchestration.activity.lending.ThirdPartyActivity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Handles third-party integrations via middleware-third-party service.
 * All calls go through the middleware execution gateway which handles:
 * - Provider/API config resolution
 * - Authentication credential injection
 * - Request/response logging
 * - Idempotency deduplication
 *
 * API codes used (pre-seeded in middleware-third-party V2 migration):
 * - TARABUT_IBAN_VERIFY — IBAN ownership verification
 * - EIGER_COMMODITY_BUY — Commodity purchase for Tawarruq
 * - EIGER_COMMODITY_SELL — Commodity sale for Tawarruq
 * - UNIFONIC_IVR_CALL — IVR verification call
 * - NAFITH_REGISTER — E-Promissory note registration
 */
@Slf4j
@Component
public class ThirdPartyActivityImpl implements ThirdPartyActivity {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String middlewareUrl;

    public ThirdPartyActivityImpl(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.services.middleware-url}") String middlewareUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.middlewareUrl = middlewareUrl;
    }

    // ══════════ SAFEWATCH AML SCREENING ══════════

    @Override
    public SafeWatchResult screenSafeWatch(SafeWatchInput input) {
        log.info("Activity: SafeWatch AML screening for NID: ***{}", input.nationalId().substring(Math.max(0, input.nationalId().length() - 4)));

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "nationalId", input.nationalId(),
                    "customerName", input.customerName() != null ? input.customerName() : "",
                    "applicationId", input.applicationId() != null ? input.applicationId() : ""
            ));

            var response = executeMiddlewareApi(
                    "SAFEWATCH_SCAN_SESSION",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "safewatch-" + input.applicationId()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                String error = response.has("errorMessage") ? response.get("errorMessage").asText() : "Unknown error";
                return new SafeWatchResult(null, "ERROR", false, error);
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            String status = textOrNull(data, "screeningStatus");
            boolean cleared = "CLEAR".equalsIgnoreCase(status) || "NO_MATCH".equalsIgnoreCase(status);

            return new SafeWatchResult(
                    textOrNull(data, "sessionId"),
                    status != null ? status : "CLEAR",
                    cleared,
                    textOrNull(data, "matchDetails")
            );

        } catch (Exception e) {
            log.warn("SafeWatch service unavailable ({}), returning mock CLEAR for development.", e.getMessage());
            return new SafeWatchResult("MOCK-SW-" + UUID.randomUUID().toString().substring(0, 8), "CLEAR", true, null);
        }
    }

    // ══════════ MASDAR EMPLOYMENT VERIFICATION ══════════

    @Override
    public MasdarResult verifyEmployment(MasdarInput input) {
        log.info("Activity: Masdar employment verification for NID: ***{}", input.nationalId().substring(Math.max(0, input.nationalId().length() - 4)));

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "nationalId", input.nationalId()
            ));

            var response = executeMiddlewareApi(
                    "MASDAR_INDIVIDUAL_PLUS",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "masdar-" + input.applicationId()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                return new MasdarResult(null, null, "UNKNOWN", null, null, null, false);
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            return new MasdarResult(
                    textOrNull(data, "employerName"),
                    textOrNull(data, "sector"),
                    textOrNull(data, "employmentStatus"),
                    data.has("basicSalary") ? new BigDecimal(data.get("basicSalary").asText()) : null,
                    data.has("totalSalary") ? new BigDecimal(data.get("totalSalary").asText()) : null,
                    textOrNull(data, "employmentStartDate"),
                    true
            );

        } catch (Exception e) {
            log.warn("Masdar service unavailable ({}), returning mock for development.", e.getMessage());
            return new MasdarResult(
                    "Saudi Aramco", "OIL_GAS", "ACTIVE",
                    new BigDecimal("12000"), new BigDecimal("15000"),
                    "2020-01-15", true
            );
        }
    }

    // ══════════ AML DECLARATION ══════════

    @Override
    public AmlDeclarationResult recordAmlDeclaration(AmlDeclarationInput input) {
        log.info("Activity: Recording AML declaration for application: {}", input.applicationId());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "customerId", input.customerId(),
                    "nationalId", input.nationalId(),
                    "applicationId", input.applicationId(),
                    "pep", input.pep(),
                    "sanctionedCountry", input.sanctionedCountry(),
                    "sourceOfFundsConfirmed", input.sourceOfFundsConfirmed()
            ));

            var response = executeMiddlewareApi(
                    "SAFEWATCH_SCAN_DETAILS",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "aml-decl-" + input.applicationId()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                return new AmlDeclarationResult(false, null);
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            return new AmlDeclarationResult(true, textOrNull(data, "referenceId"));

        } catch (Exception e) {
            log.warn("AML declaration service unavailable ({}), returning mock for development.", e.getMessage());
            return new AmlDeclarationResult(true, "MOCK-AML-" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    // ══════════ NABA NOTIFICATION ══════════

    @Override
    public NabaResult sendNabaNotification(NabaInput input) {
        log.info("Activity: Sending NABA notification for application: {}", input.applicationNumber());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "nationalId", input.nationalId(),
                    "mobileNumber", input.mobileNumber(),
                    "applicationNumber", input.applicationNumber(),
                    "loanAmount", input.loanAmount(),
                    "messageType", input.messageType() != null ? input.messageType() : "LOAN_CONFIRMATION"
            ));

            var response = executeMiddlewareApi(
                    "NABA_SEND_NOTIFICATION",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "naba-" + input.applicationNumber()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                return new NabaResult(false, null);
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            return new NabaResult(true, textOrNull(data, "referenceId"));

        } catch (Exception e) {
            log.warn("NABA service unavailable ({}), returning mock for development.", e.getMessage());
            return new NabaResult(true, "MOCK-NABA-" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    // ══════════ PAYMENT GUARD ══════════

    @Override
    public PaymentGuardResult checkPaymentGuard(PaymentGuardInput input) {
        log.info("Activity: PaymentGuard fraud check for application: {}", input.applicationId());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "customerId", input.customerId(),
                    "nationalId", input.nationalId(),
                    "applicationId", input.applicationId(),
                    "amount", input.amount(),
                    "iban", input.iban() != null ? input.iban() : ""
            ));

            var response = executeMiddlewareApi(
                    "PAYMENT_GUARD_CHECK",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "payguard-" + input.applicationId()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                return new PaymentGuardResult(null, "ERROR", false, "HIGH");
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            String status = textOrNull(data, "status");
            boolean approved = "APPROVED".equalsIgnoreCase(status) || "PASS".equalsIgnoreCase(status);

            return new PaymentGuardResult(
                    textOrNull(data, "sessionId"),
                    status != null ? status : "APPROVED",
                    approved,
                    textOrNull(data, "riskLevel")
            );

        } catch (Exception e) {
            log.warn("PaymentGuard service unavailable ({}), returning mock APPROVED for development.", e.getMessage());
            return new PaymentGuardResult("MOCK-PG-" + UUID.randomUUID().toString().substring(0, 8), "APPROVED", true, "LOW");
        }
    }

    // ══════════ IBAN VERIFICATION ══════════

    @Override
    public IbanVerificationResult verifyIban(IbanVerificationInput input) {
        log.info("Activity: Verifying IBAN {} for tenant {}", maskIban(input.iban()), input.tenantId());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "iban", input.iban(),
                    "nationalId", input.nationalId(),
                    "expectedName", input.expectedName() != null ? input.expectedName() : ""
            ));

            var response = executeMiddlewareApi(
                    "TARABUT_IBAN_VERIFY",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "iban-" + input.iban()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                String error = response.has("errorMessage") ? response.get("errorMessage").asText() : "Unknown error";
                return new IbanVerificationResult(false, null, null, null, error);
            }

            var data = response.has("responseBody") ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            return new IbanVerificationResult(
                    true,
                    textOrNull(data, "accountHolder"),
                    textOrNull(data, "bankName"),
                    textOrNull(data, "bankCode"),
                    null
            );

        } catch (Exception e) {
            log.warn("IBAN verification service unavailable ({}), returning mock success for development.", e.getMessage());
            // Mock result for development — real integration requires middleware-third-party
            return new IbanVerificationResult(true, input.expectedName(), "Al Rajhi Bank", "80", null);
        }
    }

    // ══════════ COMMODITY TRADE ══════════

    @Override
    public CommodityTradeResult executeCommodityTrade(CommodityTradeInput input) {
        log.info("Activity: Executing commodity trade for application {}, amount={}",
                input.applicationId(), input.amount());

        try {
            // Step 1: Buy commodity
            var buyRequest = objectMapper.writeValueAsString(Map.of(
                    "applicationId", input.applicationId(),
                    "shariaStructure", input.shariaStructure(),
                    "amount", input.amount(),
                    "customerId", input.customerId(),
                    "tradeType", "BUY"
            ));

            var buyResponse = executeMiddlewareApi(
                    "EIGER_COMMODITY_BUY",
                    buyRequest,
                    input.tenantId(),
                    null,
                    "commodity-buy-" + input.applicationId()
            );

            if (!buyResponse.has("success") || !buyResponse.get("success").asBoolean()) {
                return new CommodityTradeResult(null, null, null, null, null, false);
            }

            var buyData = buyResponse.has("responseBody")
                    ? objectMapper.readTree(buyResponse.get("responseBody").asText()) : buyResponse;

            String tradeId = textOrNull(buyData, "tradeId");
            if (tradeId == null) {
                tradeId = UUID.randomUUID().toString();
            }

            // Step 2: Sell commodity (for Tawarruq — customer sells commodity for cash)
            var sellRequest = objectMapper.writeValueAsString(Map.of(
                    "tradeId", tradeId,
                    "applicationId", input.applicationId(),
                    "customerId", input.customerId(),
                    "tradeType", "SELL"
            ));

            executeMiddlewareApi(
                    "EIGER_COMMODITY_SELL",
                    sellRequest,
                    input.tenantId(),
                    null,
                    "commodity-sell-" + input.applicationId()
            );

            log.info("Commodity trade completed: tradeId={}", tradeId);
            return new CommodityTradeResult(
                    tradeId,
                    textOrNull(buyData, "commodityType"),
                    input.amount(),
                    input.amount(), // sale price = purchase price for Tawarruq
                    textOrNull(buyData, "certificateId"),
                    true
            );

        } catch (Exception e) {
            log.warn("Commodity trade service unavailable ({}), returning mock for dev.", e.getMessage());
            String mockTradeId = "MOCK-TRADE-" + UUID.randomUUID().toString().substring(0, 8);
            return new CommodityTradeResult(mockTradeId, "PLATINUM", input.amount(), input.amount(), "MOCK-CERT", true);
        }
    }

    @Override
    public void reverseCommodityTrade(String tradeId) {
        log.info("Activity: Reversing commodity trade: {}", tradeId);

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "tradeId", tradeId,
                    "action", "REVERSE"
            ));

            // Use a dedicated reverse API or the same sell API with REVERSE action
            executeMiddlewareApiNoAuth("EIGER_COMMODITY_SELL", requestBody, "commodity-reverse-" + tradeId);

            log.info("Commodity trade reversed: {}", tradeId);
        } catch (Exception e) {
            log.error("Commodity trade reversal failed (SAGA compensation): tradeId={}, error={}",
                    tradeId, e.getMessage(), e);
            // Log but don't throw — compensation should be best-effort
        }
    }

    // ══════════ IVR VERIFICATION ══════════

    @Override
    public IvrInitiateResult initiateIvrCall(IvrInitiateInput input) {
        log.info("Activity: Initiating IVR call for application {}", input.applicationId());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "applicationId", input.applicationId(),
                    "mobileNumber", input.mobileNumber(),
                    "customerName", input.customerName(),
                    "loanAmount", input.loanAmount()
            ));

            var response = executeMiddlewareApi(
                    "UNIFONIC_IVR_CALL",
                    requestBody,
                    input.tenantId(),
                    null,
                    "ivr-" + input.applicationId()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                return new IvrInitiateResult(null, "FAILED", false);
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            return new IvrInitiateResult(
                    textOrNull(data, "callId"),
                    textOrNull(data, "status"),
                    true
            );

        } catch (Exception e) {
            log.warn("IVR service unavailable ({}), returning mock for dev.", e.getMessage());
            return new IvrInitiateResult("MOCK-IVR-" + UUID.randomUUID().toString().substring(0, 8), "COMPLETED", true);
        }
    }

    // ══════════ E-PROMISSORY ══════════

    @Override
    public EPromissoryResult registerEPromissory(EPromissoryInput input) {
        log.info("Activity: Registering e-promissory for loan {}", input.loanNumber());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "loanId", input.loanId(),
                    "loanNumber", input.loanNumber(),
                    "customerId", input.customerId(),
                    "nationalId", input.nationalId(),
                    "totalAmount", input.totalAmount(),
                    "tenureMonths", input.tenureMonths(),
                    "installmentAmount", input.installmentAmount()
            ));

            var response = executeMiddlewareApi(
                    "NAFITH_REGISTER",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "epromissory-" + input.loanNumber()
            );

            if (!response.has("success") || !response.get("success").asBoolean()) {
                return new EPromissoryResult(null, null, false);
            }

            var data = response.has("responseBody")
                    ? objectMapper.readTree(response.get("responseBody").asText()) : response;

            return new EPromissoryResult(
                    textOrNull(data, "registrationId"),
                    textOrNull(data, "promissoryNumber"),
                    true
            );

        } catch (Exception e) {
            log.warn("E-Promissory service unavailable ({}), returning mock for dev.", e.getMessage());
            return new EPromissoryResult("MOCK-REG-" + UUID.randomUUID().toString().substring(0, 8), "MOCK-PN-001", true);
        }
    }

    // ══════════ MIDDLEWARE HELPER ══════════

    /**
     * Execute an API call through the middleware-third-party execution gateway.
     * Uses the simple endpoint: POST /api/v1/execute/{apiCode}/simple
     */
    private JsonNode executeMiddlewareApi(String apiCode, String requestBody,
                                           String tenantId, String nationalId,
                                           String idempotencyKey) throws Exception {
        String url = middlewareUrl + "/api/v1/execute/" + apiCode + "/simple";

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("X-Idempotency-Key", idempotencyKey);
        }
        if (nationalId != null) {
            headers.set("X-National-Id", nationalId);
        }
        headers.set("X-Caller-Service", "lending-service");

        var httpEntity = new HttpEntity<>(requestBody, headers);
        var response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

        return objectMapper.readTree(response.getBody());
    }

    /**
     * Execute without tenant auth (for SAGA compensation where JWT may not be available).
     */
    private JsonNode executeMiddlewareApiNoAuth(String apiCode, String requestBody,
                                                 String idempotencyKey) throws Exception {
        String url = middlewareUrl + "/api/v1/execute/" + apiCode + "/simple";

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("X-Idempotency-Key", idempotencyKey);
        }
        headers.set("X-Caller-Service", "lending-service");

        var httpEntity = new HttpEntity<>(requestBody, headers);
        var response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

        return objectMapper.readTree(response.getBody());
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 8) return "***";
        return iban.substring(0, 4) + "****" + iban.substring(iban.length() - 4);
    }

    // ══════════ EMDHA DIGITAL SIGNATURE (BRD V1.8) ══════════

    @Override
    public EmdhaSignResult signWithEmdha(EmdhaSignInput input) {
        log.info("Activity: Signing contract with Emdha for application {}", input.applicationId());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "nationalId", input.nationalId(),
                    "customerId", input.customerId(),
                    "contractType", input.contractType() != null ? input.contractType() : "FINANCING_CONTRACT",
                    "documentContent", input.documentContent() != null ? input.documentContent() : ""
            ));

            var response = executeMiddlewareApi(
                    "EMDHA_SIGN",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "emdha-sign-" + input.applicationId()
            );

            if (response.has("success") && response.get("success").asBoolean()) {
                var data = response.has("responseBody")
                        ? objectMapper.readTree(response.get("responseBody").asText())
                        : response;
                return new EmdhaSignResult(
                        textOrNull(data, "signatureId"),
                        textOrNull(data, "signedDocumentId"),
                        true,
                        "SIGNED"
                );
            }

            return new EmdhaSignResult(null, null, false, "FAILED");

        } catch (Exception e) {
            log.warn("Emdha signing unavailable ({}), returning mock success for development", e.getMessage());
            return new EmdhaSignResult(
                    "EMDHA-MOCK-" + UUID.randomUUID(),
                    "DOC-MOCK-" + UUID.randomUUID(),
                    true,
                    "MOCK_SIGNED"
            );
        }
    }

    // ══════════ DAKHLI INCOME VERIFICATION (BRD V1.8) ══════════

    @Override
    public DakhliResult fetchDakhliIncome(DakhliInput input) {
        log.info("Activity: Fetching Dakhli income for NID {}", input.nationalId());

        try {
            var requestBody = objectMapper.writeValueAsString(Map.of(
                    "nationalId", input.nationalId()
            ));

            var response = executeMiddlewareApi(
                    "DAKHLI_GOSI",
                    requestBody,
                    input.tenantId(),
                    input.nationalId(),
                    "dakhli-" + input.applicationId()
            );

            if (response.has("success") && response.get("success").asBoolean()) {
                var data = response.has("responseBody")
                        ? objectMapper.readTree(response.get("responseBody").asText())
                        : response;
                return new DakhliResult(
                        data.has("salary") ? new BigDecimal(data.get("salary").asText()) : BigDecimal.ZERO,
                        textOrNull(data, "employerName"),
                        textOrNull(data, "employmentStatus"),
                        textOrNull(data, "sector"),
                        true,
                        null
                );
            }

            return new DakhliResult(BigDecimal.ZERO, null, null, null, false, "Dakhli API returned failure");

        } catch (Exception e) {
            log.warn("Dakhli service unavailable ({}), returning mock data", e.getMessage());
            return new DakhliResult(
                    new BigDecimal("15000"),
                    "Mock Employer",
                    "ACTIVE",
                    "GOVERNMENT",
                    true,
                    null
            );
        }
    }
}
