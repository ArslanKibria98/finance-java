package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.adapter.rest.request.EvaluateEligibilityRequest;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;
import com.ksa.financing.risk.domain.port.in.EvaluateEligibilityUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service credit scoring decision endpoint. Bypasses the user JWT
 * filter chain (mounted under {@code /internal/**} which is permitAll in
 * SecurityConfig). Tenant is supplied via {@code X-Tenant-Id} header set by
 * the calling service (e.g. lending workflow activity).
 */
@RestController
@RequestMapping("/internal/credit-scoring")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Credit Scoring", description = "Service-to-service credit decisioning")
public class InternalCreditScoringController {

    private final EvaluateEligibilityUseCase evaluateEligibilityUseCase;

    @PostMapping("/products/{productId}/evaluate")
    @Operation(summary = "Run credit decision engine (internal, service-to-service)",
            description = "Evaluates customer answers against product scoring rules and returns "
                    + "a Green/Amber/Red decision. Tenant comes from X-Tenant-Id header.")
    public EligibilityEvaluationResult evaluateInternal(
            @PathVariable UUID productId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody EvaluateEligibilityRequest request) {
        log.info("[internal] Evaluating credit decision for product={} tenant={} green={} amber={}",
                productId, tenantId, request.greenThreshold(), request.amberThreshold());
        return evaluateEligibilityUseCase.evaluate(
                tenantId, productId, request.answers(),
                request.greenThreshold(), request.amberThreshold());
    }
}
