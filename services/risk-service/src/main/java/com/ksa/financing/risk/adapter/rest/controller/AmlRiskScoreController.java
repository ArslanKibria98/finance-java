package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.application.dto.AmlRiskScoreRequestDto;
import com.ksa.financing.risk.application.dto.AmlRiskScoreResponseDto;
import com.ksa.financing.risk.domain.model.aml.AmlRiskScore;
import com.ksa.financing.risk.domain.model.aml.AmlScoringInput;
import com.ksa.financing.risk.domain.port.in.CalculateAmlRiskScoreUseCase;
import com.ksa.financing.risk.domain.port.out.AmlRiskAssessmentRepository;
import com.ksa.financing.risk.infrastructure.persistence.AmlRiskAssessmentRepositoryImpl;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for AML Risk Scoring.
 *
 * Public API (no JWT required) — called by onboarding workflow via Temporal activity.
 * Tenant identification is via X-Tenant-Id header (set by API gateway or caller).
 */
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AML Risk Scoring", description = "AML/CFT risk assessment based on EastNets scoring model")
public class AmlRiskScoreController {

    private final CalculateAmlRiskScoreUseCase calculateAmlRiskScoreUseCase;
    private final AmlRiskAssessmentRepository amlRiskAssessmentRepository;

    @PostMapping("/aml-score")
    @Operation(summary = "Calculate AML risk score",
        description = "Calculates AML risk score for a customer based on their profile data. " +
                "Uses the EastNets weighted scoring model with 8 risk categories. " +
                "Supports idempotency via idempotencyKey field.")
    public AmlRiskScoreResponseDto calculateAmlScore(
            @Valid @RequestBody AmlRiskScoreRequestDto request,
            HttpServletRequest httpRequest) {

        String tenantId = extractTenantId(httpRequest);
        log.info("Calculating AML risk score for tenant={}, nidHash=...{}",
                tenantId, maskHash(request.nationalIdHash()));

        var input = new AmlScoringInput(
                request.nationalIdHash(),
                request.nationality(),
                request.cityName(),
                request.occupationCode(),
                request.monthlyIncome(),
                request.sourceOfIncome(),
                request.productRiskTier(),
                request.isPep(),
                request.isOnInternalList(),
                tenantId,
                request.customerId(),
                request.idempotencyKey()
        );

        AmlRiskScore score = calculateAmlRiskScoreUseCase.calculate(input);

        log.info("AML risk score calculated: assessmentId={}, totalScore={}, riskLevel={}, dominantOverride={}",
                score.assessmentId(), score.totalScore(), score.riskLevel(), score.dominantOverride());

        return AmlRiskScoreResponseDto.from(score);
    }

    @GetMapping("/aml-score/customer/{customerId}")
    @Operation(summary = "Get AML risk assessments by customer ID",
        description = "Retrieves all AML risk assessments for a customer by their UUID. " +
                "Includes inputData (compliance answers) and score breakdown. " +
                "Used by Customer 360 view to aggregate risk data.")
    @SuppressWarnings("unchecked")
    public List<AmlRiskScoreResponseDto> getByCustomerId(@PathVariable String customerId) {
        log.debug("Fetching AML assessments for customerId={}", customerId);
        if (!(amlRiskAssessmentRepository instanceof AmlRiskAssessmentRepositoryImpl impl)) {
            return amlRiskAssessmentRepository.findAllByCustomerId(customerId)
                    .stream().map(AmlRiskScoreResponseDto::from).toList();
        }
        return impl.findAllWithInputDataByCustomerId(customerId).stream()
                .map(entry -> AmlRiskScoreResponseDto.fromWithInputData(
                        (AmlRiskScore) entry.get("score"),
                        (java.util.Map<String, Object>) entry.get("inputData")))
                .toList();
    }

    private String extractTenantId(HttpServletRequest request) {
        var tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "X-Tenant-Id header is required");
        }
        try {
            UUID.fromString(tenantHeader);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "X-Tenant-Id header must be a valid UUID");
        }
        return tenantHeader;
    }

    private String maskHash(String hash) {
        if (hash == null || hash.length() < 8) return "***";
        return "..." + hash.substring(hash.length() - 8);
    }
}
