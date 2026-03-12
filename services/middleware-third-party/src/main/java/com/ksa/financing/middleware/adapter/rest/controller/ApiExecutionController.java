package com.ksa.financing.middleware.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.middleware.application.dto.ExecuteApiRequest;
import com.ksa.financing.middleware.application.dto.ExecuteApiResponse;
import com.ksa.financing.middleware.domain.port.in.ExecuteApiUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public API execution gateway.
 * Authentication is via client secretKey (in request body), not JWT.
 * Client access to specific APIs is verified by the use case.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
@Tag(name = "API Execution", description = "Execute third-party API calls through the middleware gateway")
public class ApiExecutionController {

    private final ExecuteApiUseCase executeApiUseCase;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Execute a third-party API call by its registered API code")
    @PostMapping("/{apiCode}")
    public ResponseEntity<ExecuteApiResponse> execute(
            @PathVariable String apiCode,
            @RequestBody ExecuteApiRequest request) {

        var result = executeApiUseCase.execute(
                request.secretKey(),
                apiCode,
                request.requestBody(),
                request.pathParams() != null ? request.pathParams() : Map.of(),
                request.queryParams() != null ? request.queryParams() : Map.of(),
                request.headers() != null ? request.headers() : Map.of(),
                request.idempotencyKey(),
                request.nationalId(),
                request.callerService()
        );

        var response = new ExecuteApiResponse(
                result.requestId(),
                result.httpStatus(),
                parseResponseBody(result.responseBody()),
                result.durationMs(),
                result.success(),
                result.errorMessage()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Simple execution with just a JSON body and secret key in header")
    @PostMapping("/{apiCode}/simple")
    public ResponseEntity<ExecuteApiResponse> executeSimple(
            @PathVariable String apiCode,
            @RequestBody(required = false) String requestBody,
            @RequestHeader(value = "X-Secret-Key") String secretKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-National-Id", required = false) String nationalId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService) {

        var result = executeApiUseCase.execute(
                secretKey,
                apiCode,
                requestBody,
                Map.of(),
                Map.of(),
                Map.of(),
                idempotencyKey,
                nationalId,
                callerService
        );

        var response = new ExecuteApiResponse(
                result.requestId(),
                result.httpStatus(),
                parseResponseBody(result.responseBody()),
                result.durationMs(),
                result.success(),
                result.errorMessage()
        );

        return ResponseEntity.ok(response);
    }

    private Object parseResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            return responseBody;
        }
    }
}
