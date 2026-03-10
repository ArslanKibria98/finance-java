package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.application.dto.fraud.FraudEvaluationResponseDto;
import com.ksa.financing.risk.application.dto.fraud.FraudEventRequestDto;
import com.ksa.financing.risk.application.mapper.FraudEventMapper;
import com.ksa.financing.risk.domain.port.in.EvaluateFraudEventUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Fraud evaluation endpoint — receives events from LOS/LMS and returns fraud decision.
 * Public endpoint (no JWT) — called by internal services during onboarding/transaction flow.
 * Tenant extracted from X-Tenant-Id header.
 */
@RestController
@RequestMapping("/api/v1/risk/fraud")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fraud Evaluation", description = "Real-time fraud event evaluation engine")
public class FraudEvaluationController {

    private final EvaluateFraudEventUseCase evaluateFraudEventUseCase;

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate fraud event",
            description = "Evaluates a fraud event against all active rules and returns decision (ALLOW/ALERT/HOLD/BLOCK)")
    public ResponseEntity<FraudEvaluationResponseDto> evaluate(
            @RequestHeader("X-Tenant-Id") String tenantIdHeader,
            @Valid @RequestBody FraudEventRequestDto request) {

        var tenantId = parseTenantId(tenantIdHeader);
        log.info("Fraud evaluation request eventId={} type={} customer={} tenant={}",
                request.eventId(), request.eventType(), request.customerId(), tenantId);

        var event = FraudEventMapper.toDomain(tenantId, request);
        var result = evaluateFraudEventUseCase.evaluate(tenantId, event);
        var response = FraudEventMapper.toResponseDto(result);

        return ResponseEntity.ok(response);
    }

    private UUID parseTenantId(String tenantIdHeader) {
        if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "X-Tenant-Id header is required");
        }
        try {
            return UUID.fromString(tenantIdHeader);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Invalid X-Tenant-Id format: " + tenantIdHeader);
        }
    }
}
