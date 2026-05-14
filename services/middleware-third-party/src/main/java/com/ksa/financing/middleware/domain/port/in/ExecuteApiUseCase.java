package com.ksa.financing.middleware.domain.port.in;

import java.util.Map;
import java.util.UUID;

/**
 * Business-context attribution attached to a middleware call so cost can be
 * aggregated per customer, per loan application, or per onboarding session.
 */

/**
 * Input port for executing third-party API calls through the middleware gateway.
 * Resolves provider config, builds HTTP request, executes with resilience, and logs everything.
 */
public interface ExecuteApiUseCase {

    /**
     * Execute a third-party API call by its registered API code.
     * Client is authenticated via secretKey. tenantId is resolved from the client record.
     *
     * @param secretKey      client secret key for authentication
     * @param apiCode        registered ProviderApi code (e.g., "SIMAH_CREDIT_CHECK", "EIGER_COMMODITY_BUY")
     * @param requestBody    JSON request payload (may be null for GET requests)
     * @param pathParams     path parameter substitutions (e.g., {id} → value)
     * @param queryParams    additional query parameters to merge with config
     * @param headers        additional headers to merge with config
     * @param idempotencyKey optional idempotency key for deduplication
     * @param nationalId     optional NID for audit trail
     * @param mobileNumber   optional mobile number for audit trail (OTP/IVR APIs)
     * @param callerService  calling service identifier (e.g., "lending-service")
     * @return execution result with response data
     */
    ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                            Map<String, String> pathParams, Map<String, String> queryParams,
                            Map<String, String> headers, String idempotencyKey,
                            String nationalId, String mobileNumber, String callerService);

    /**
     * Extended entry point that captures business context (customer / application /
     * onboarding) so per-customer cost reports can be produced downstream.
     */
    ExecutionResult execute(String secretKey, String apiCode, String requestBody,
                            Map<String, String> pathParams, Map<String, String> queryParams,
                            Map<String, String> headers, String idempotencyKey,
                            String nationalId, String mobileNumber, String callerService,
                            BusinessContext context);

    record BusinessContext(UUID customerId, String applicationId, String contextType) {
        public static BusinessContext empty() {
            return new BusinessContext(null, null, null);
        }
    }

    record ExecutionResult(
            String requestId,
            int httpStatus,
            String responseBody,
            String responseHeaders,
            long durationMs,
            boolean success,
            String errorMessage
    ) {}
}
