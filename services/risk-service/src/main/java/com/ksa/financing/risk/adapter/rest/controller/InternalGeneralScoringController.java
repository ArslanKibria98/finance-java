package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.adapter.rest.request.EvaluateGeneralScoringRequest;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.EligibilityEvaluationResult;
import com.ksa.financing.risk.domain.port.in.EvaluateGeneralScoringUseCase;
import com.ksa.financing.risk.domain.port.in.EvaluateGeneralScoringUseCase.EvaluateInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Service-to-service general-credit-scoring endpoint. Called by the onboarding
 * workflow's Temporal activity after each third-party stage to update the
 * customer's incremental score.
 *
 * Mounted under {@code /internal/**} (permitAll in SecurityConfig); tenant is
 * supplied via {@code X-Tenant-Id} header. By default, persists the resulting
 * snapshot + updates the customer's cached current score.
 */
@RestController
@RequestMapping("/internal/credit-scoring/general")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal General Credit Scoring",
        description = "Service-to-service general (onboarding) scoring")
public class InternalGeneralScoringController {

    private final EvaluateGeneralScoringUseCase useCase;

    @PostMapping("/evaluate")
    @Operation(summary = "Run general scoring engine (internal)",
            description = "Evaluates current onboarding answers against general scoring rules. "
                    + "Persists a snapshot + updates current cache by default unless "
                    + "persistSnapshot=false is sent.")
    public EligibilityEvaluationResult evaluateInternal(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @Valid @RequestBody EvaluateGeneralScoringRequest request) {
        log.info("[internal] Evaluating general scoring tenant={} customer={} stage={}",
                tenantId, request.customerId(), request.stage());

        // For service-to-service calls, default persistSnapshot to TRUE unless caller explicitly set false.
        boolean persist = request.persistSnapshot() == null || request.persistSnapshot();

        return useCase.evaluate(new EvaluateInput(
                tenantId,
                request.customerId(),
                request.workflowId(),
                request.stage(),
                request.answers() == null ? Map.of() : request.answers(),
                persist
        ));
    }

    @GetMapping("/customers/{customerId}/current")
    @Operation(summary = "Get current cached general score for a customer (internal)",
            description = "Service-to-service GET for lending-service (and others) to look up "
                    + "the customer's latest general credit decision without going through JWT. "
                    + "Returns 404 if the customer has no scoring snapshot yet.")
    public ResponseEntity<CustomerCreditScoreCurrent> getCurrentScoreInternal(
            @PathVariable UUID customerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.debug("[internal] Looking up current general score tenant={} customer={}",
                tenantId, customerId);
        return useCase.getCurrentScore(tenantId, customerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
