package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.risk.application.dto.AmlRiskScoreResponseDto;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreSnapshot;
import com.ksa.financing.risk.domain.port.in.EvaluateGeneralScoringUseCase;
import com.ksa.financing.risk.domain.port.out.AmlRiskAssessmentRepository;
import com.ksa.financing.risk.infrastructure.persistence.AmlRiskAssessmentRepositoryImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Customer-centric risk detail aggregator.
 *
 * <p>Single endpoint that returns EVERYTHING the Risk History UI needs in one
 * call: the customer's general (onboarding) credit score current snapshot +
 * history, plus all AML risk assessments.
 *
 * <p>Without this, the frontend would have to fan out to 3 endpoints
 * ({@code /aml-score/customer/{id}}, {@code /credit-scoring/general/customers/{id}/current},
 * {@code /credit-scoring/general/customers/{id}/history}) and merge them.
 */
@RestController
@RequestMapping("/api/v1/risk/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Risk Detail",
        description = "Aggregated risk view for a customer (AML + general credit scoring)")
public class CustomerRiskDetailController {

    private final AmlRiskAssessmentRepository amlRepository;
    private final EvaluateGeneralScoringUseCase generalScoringUseCase;

    @SecuredEndpoint(obj = "risk.customer-risk-detail", act = "read")
    @GetMapping("/{customerId}/risk-detail")
    @Operation(summary = "Get full risk detail for a customer (AML + general credit scoring)",
            description = "Returns one consolidated payload with current general credit score, "
                    + "snapshot history (one per onboarding stage with full per-criterion "
                    + "breakdown), and all AML risk assessments. Used by the Risk History UI.")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRiskDetail(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Risk detail requested for customer={} tenant={}", customerId, tenantId);

        // 1. General credit scoring — current cached summary
        CustomerCreditScoreCurrent current = generalScoringUseCase
                .getCurrentScore(tenantId, customerId).orElse(null);

        // 2. General credit scoring — full snapshot history (per stage, with breakdown)
        List<CustomerCreditScoreSnapshot> history = generalScoringUseCase
                .getSnapshotHistory(tenantId, customerId);

        // 3. AML risk assessments (per session)
        List<AmlRiskScoreResponseDto> amlAssessments;
        if (amlRepository instanceof AmlRiskAssessmentRepositoryImpl impl) {
            amlAssessments = impl.findAllWithInputDataByCustomerId(customerId.toString()).stream()
                    .map(entry -> AmlRiskScoreResponseDto.fromWithInputData(
                            (com.ksa.financing.risk.domain.model.aml.AmlRiskScore) entry.get("score"),
                            (Map<String, Object>) entry.get("inputData")))
                    .toList();
        } else {
            amlAssessments = amlRepository.findAllByCustomerId(customerId.toString())
                    .stream().map(AmlRiskScoreResponseDto::from).toList();
        }

        return Map.of(
                "customerId", customerId,
                "tenantId", tenantId,
                "generalCreditScoring", Map.of(
                        "current", current == null ? Map.of() : current,
                        "history", history,
                        "totalSnapshots", history.size()
                ),
                "amlRisk", Map.of(
                        "assessments", amlAssessments,
                        "totalAssessments", amlAssessments.size()
                )
        );
    }

    private UUID extractTenantId(Jwt jwt) {
        var claim = jwt.getClaimAsString("tenant_id");
        if (claim == null || claim.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(claim);
    }
}
