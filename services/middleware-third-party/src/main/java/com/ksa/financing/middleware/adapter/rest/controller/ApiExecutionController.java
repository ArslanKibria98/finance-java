package com.ksa.financing.middleware.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Authentication is via X-Secret-Key header, not JWT.
 * Metadata (nationalId, callerService) passed via headers.
 * Body contains only the raw API payload.
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
