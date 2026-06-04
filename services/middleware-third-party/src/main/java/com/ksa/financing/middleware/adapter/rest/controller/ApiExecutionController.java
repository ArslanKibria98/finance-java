package com.ksa.financing.middleware.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.middleware.application.dto.ExecuteApiResponse;
import com.ksa.financing.middleware.domain.port.in.ExecuteApiUseCase;
import com.ksa.financing.middleware.domain.port.in.ExecuteApiUseCase.BusinessContext;
import com.ksa.financing.middleware.domain.port.out.ClientRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Third-party API execution gateway. Two flavors:
 *
 *  - POST /api/v1/execute/{apiCode}
 *      External / authenticated callers. Requires X-Secret-Key header
 *      identifying the calling ApiClient.
 *
 *  - POST /api/v1/execute/{apiCode}/simple
 *      Internal service-to-service calls (lending-service,
 *      onboarding-workflow-service, etc.). No X-Secret-Key required —
 *      the middleware resolves the system default client by code from
 *      `app.middleware.default-client-code` and reuses its secret + env.
 *      Locked down at the SecurityConfig level to the internal network.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
@Tag(name = "API Execution", description = "Execute third-party API calls through the middleware gateway")
public class ApiExecutionController {

    private final ExecuteApiUseCase executeApiUseCase;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.middleware.default-client-code:TEST_MOCK_CLIENT}")
    private String defaultClientCode;

    @Operation(summary = "Execute a third-party API call by its registered API code (external)")
    @PostMapping("/{apiCode}")
    public ResponseEntity<ExecuteApiResponse> execute(
            @PathVariable String apiCode,
            @RequestBody(required = false) String requestBody,
            @RequestHeader(value = "X-Secret-Key") String secretKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-National-Id", required = false) String nationalId,
            @RequestHeader(value = "X-Mobile-Number", required = false) String mobileNumber,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Customer-Id", required = false) String customerId,
            @RequestHeader(value = "X-Application-Id", required = false) String applicationId,
            @RequestHeader(value = "X-Context-Type", required = false) String contextType,
            @RequestHeader(value = "X-Path-Params", required = false) String pathParamsJson,
            @RequestHeader(value = "X-Query-Params", required = false) String queryParamsJson,
            @RequestHeader(value = "X-Forward-Headers", required = false) String forwardHeadersJson) {

        var result = executeApiUseCase.execute(
                secretKey, apiCode, requestBody,
                parseJsonMap(pathParamsJson), parseJsonMap(queryParamsJson), parseJsonMap(forwardHeadersJson),
                idempotencyKey, nationalId, mobileNumber, callerService,
                buildContext(customerId, applicationId, contextType));

        return ResponseEntity.ok(buildResponse(result));
    }

    @Operation(summary = "Internal service-to-service execution (system-managed client)",
            description = "No X-Secret-Key required. Middleware resolves the default system " +
                    "client from `app.middleware.default-client-code` and routes to the " +
                    "client_request_{test|dev|prod} table based on that client's environment.")
    @PostMapping("/{apiCode}/simple")
    public ResponseEntity<ExecuteApiResponse> executeSimple(
            @PathVariable String apiCode,
            @RequestBody(required = false) String requestBody,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-National-Id", required = false) String nationalId,
            @RequestHeader(value = "X-Mobile-Number", required = false) String mobileNumber,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Customer-Id", required = false) String customerId,
            @RequestHeader(value = "X-Application-Id", required = false) String applicationId,
            @RequestHeader(value = "X-Context-Type", required = false) String contextType,
            @RequestHeader(value = "X-Path-Params", required = false) String pathParamsJson,
            @RequestHeader(value = "X-Query-Params", required = false) String queryParamsJson,
            @RequestHeader(value = "X-Forward-Headers", required = false) String forwardHeadersJson) {

        var systemClient = clientRepository.findByCode(defaultClientCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND,
                        "Default middleware client not configured or missing: " + defaultClientCode));

        log.debug("Simple execution via system client: code={}, env={}, apiCode={}, caller={}, customer={}, app={}, ctx={}",
                systemClient.getCode(), systemClient.getEnvironment(), apiCode, callerService,
                customerId, applicationId, contextType);

        var result = executeApiUseCase.execute(
                systemClient.getSecretKey(), apiCode, requestBody,
                parseJsonMap(pathParamsJson), parseJsonMap(queryParamsJson), parseJsonMap(forwardHeadersJson),
                idempotencyKey, nationalId, mobileNumber, callerService,
                buildContext(customerId, applicationId, contextType));

        return ResponseEntity.ok(buildResponse(result));
    }

    @Operation(summary = "Execute a multipart file-upload API call (external)",
            description = "For providers whose endpoint expects multipart/form-data (e.g. Sullis " +
                    "document / selfie upload). Path + query params are passed as text form fields.")
    @PostMapping(value = "/{apiCode}/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExecuteApiResponse> executeMultipart(
            @PathVariable String apiCode,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "formField", defaultValue = "file") String formField,
            @RequestParam(value = "pathParams", required = false) String pathParamsJson,
            @RequestParam(value = "queryParams", required = false) String queryParamsJson,
            @RequestHeader(value = "X-Secret-Key") String secretKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-National-Id", required = false) String nationalId,
            @RequestHeader(value = "X-Mobile-Number", required = false) String mobileNumber,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Customer-Id", required = false) String customerId,
            @RequestHeader(value = "X-Application-Id", required = false) String applicationId,
            @RequestHeader(value = "X-Context-Type", required = false) String contextType) {

        var result = executeApiUseCase.executeMultipart(
                secretKey, apiCode, toMultipartPart(file, formField),
                parseJsonMap(pathParamsJson), parseJsonMap(queryParamsJson), Map.of(),
                idempotencyKey, nationalId, mobileNumber, callerService,
                buildContext(customerId, applicationId, contextType));

        return ResponseEntity.ok(buildResponse(result));
    }

    @Operation(summary = "Internal multipart file-upload execution (system-managed client)",
            description = "No X-Secret-Key required. Same as /multipart but routes via the " +
                    "default system client (used by onboarding-workflow-service for Sullis uploads).")
    @PostMapping(value = "/{apiCode}/simple/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExecuteApiResponse> executeSimpleMultipart(
            @PathVariable String apiCode,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "formField", defaultValue = "file") String formField,
            @RequestParam(value = "pathParams", required = false) String pathParamsJson,
            @RequestParam(value = "queryParams", required = false) String queryParamsJson,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-National-Id", required = false) String nationalId,
            @RequestHeader(value = "X-Mobile-Number", required = false) String mobileNumber,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Customer-Id", required = false) String customerId,
            @RequestHeader(value = "X-Application-Id", required = false) String applicationId,
            @RequestHeader(value = "X-Context-Type", required = false) String contextType) {

        var systemClient = clientRepository.findByCode(defaultClientCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND,
                        "Default middleware client not configured or missing: " + defaultClientCode));

        log.debug("Simple multipart execution via system client: code={}, env={}, apiCode={}, caller={}, file={}",
                systemClient.getCode(), systemClient.getEnvironment(), apiCode, callerService,
                file.getOriginalFilename());

        var result = executeApiUseCase.executeMultipart(
                systemClient.getSecretKey(), apiCode, toMultipartPart(file, formField),
                parseJsonMap(pathParamsJson), parseJsonMap(queryParamsJson), Map.of(),
                idempotencyKey, nationalId, mobileNumber, callerService,
                buildContext(customerId, applicationId, contextType));

        return ResponseEntity.ok(buildResponse(result));
    }

    /** Parse a JSON object string ({@code {"k":"v"}}) into a String map; empty map if null/blank/invalid. */
    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Ignoring invalid JSON map param: {}", e.getMessage());
            return Map.of();
        }
    }

    private ExecuteApiUseCase.MultipartPart toMultipartPart(MultipartFile file, String formField) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Multipart 'file' part is required");
        }
        try {
            return new ExecuteApiUseCase.MultipartPart(
                    formField,
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType());
        } catch (java.io.IOException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "Failed to read uploaded file: " + e.getMessage());
        }
    }

    private BusinessContext buildContext(String customerId, String applicationId, String contextType) {
        UUID customerUuid = null;
        if (customerId != null && !customerId.isBlank()) {
            try {
                customerUuid = UUID.fromString(customerId);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring invalid X-Customer-Id header: {}", customerId);
            }
        }
        String app = (applicationId != null && !applicationId.isBlank()) ? applicationId : null;
        String ctx = (contextType != null && !contextType.isBlank()) ? contextType.toUpperCase() : null;
        return new BusinessContext(customerUuid, app, ctx);
    }

    private ExecuteApiResponse buildResponse(ExecuteApiUseCase.ExecutionResult result) {
        return new ExecuteApiResponse(
                result.requestId(),
                result.httpStatus(),
                parseResponseBody(result.responseBody()),
                result.durationMs(),
                result.success(),
                result.errorMessage()
        );
    }

    private Object parseResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            return responseBody;
        }
    }
}
