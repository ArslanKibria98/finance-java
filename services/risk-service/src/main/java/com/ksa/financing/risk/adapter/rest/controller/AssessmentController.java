package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.assessment.AssessmentAnswer;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;
import com.ksa.financing.risk.domain.model.assessment.ScoreBreakdown;
import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.port.in.RunAssessmentUseCase;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/assessments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Assessment", description = "APIs for managing assessment sessions and scoring")
public class AssessmentController {

    private final RunAssessmentUseCase runAssessmentUseCase;

    @SecuredEndpoint(obj = "risk.assessment", act = "create")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate a new risk assessment session")
    public AssessmentSession initiateSession(
            @Valid @RequestBody InitiateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        var riskType = RiskType.valueOf(request.riskType());
        log.info("Initiating assessment session for entity: {} riskType: {} tenant: {}",
                request.entityReference(), riskType, tenantId);
        var command = new RunAssessmentUseCase.InitiateSessionCommand(
                riskType, request.entityReference(), request.idempotencyKey());
        return runAssessmentUseCase.initiateSession(tenantId, command);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "create")
    @PostMapping("/{sessionId}/answers")
    @Operation(summary = "Submit answers for an assessment session")
    public AssessmentSession submitAnswers(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswersRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID actorId = UUID.fromString(jwt.getSubject());
        log.info("Submitting {} answers for session: {} tenant: {}", request.answers().size(), sessionId, tenantId);
        var commands = request.answers().stream()
                .map(a -> new RunAssessmentUseCase.SubmitAnswerCommand(
                        a.parameterId(), a.answerValue(), a.languageCode()))
                .toList();
        return runAssessmentUseCase.submitAnswers(tenantId, sessionId, commands, actorId);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "create")
    @PostMapping("/{sessionId}/draft")
    @Operation(summary = "Save answers as draft for an assessment session")
    public AssessmentSession saveDraft(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswersRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID actorId = UUID.fromString(jwt.getSubject());
        log.info("Saving draft for session: {} tenant: {}", sessionId, tenantId);
        var commands = request.answers().stream()
                .map(a -> new RunAssessmentUseCase.SubmitAnswerCommand(
                        a.parameterId(), a.answerValue(), a.languageCode()))
                .toList();
        return runAssessmentUseCase.saveDraft(tenantId, sessionId, commands, actorId);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "manage")
    @PostMapping("/{sessionId}/score")
    @Operation(summary = "Score an assessment session")
    public AssessmentSession scoreSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Scoring session: {} tenant: {}", sessionId, tenantId);
        return runAssessmentUseCase.scoreSession(tenantId, sessionId);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "read")
    @GetMapping("/entity/{entityReference}")
    @Operation(summary = "Get all assessment sessions for an entity (paginated)")
    public PageResponse<AssessmentSession> getSessionsByEntity(
            @PathVariable String entityReference,
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = isSuperAdmin(jwt) ? null : extractTenantId(jwt);
        return runAssessmentUseCase.getSessionsByEntity(tenantId, entityReference, pageQuery);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "read")
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get an assessment session by ID")
    public AssessmentSession getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = isSuperAdmin(jwt) ? null : extractTenantId(jwt);
        return runAssessmentUseCase.getSession(tenantId, sessionId);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "read")
    @GetMapping("/{sessionId}/answers")
    @Operation(summary = "Get all answers for an assessment session")
    public List<AssessmentAnswer> getAnswersBySession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = isSuperAdmin(jwt) ? null : extractTenantId(jwt);
        return runAssessmentUseCase.getAnswersBySession(tenantId, sessionId);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "read")
    @GetMapping("/{sessionId}/breakdown")
    @Operation(summary = "Get score breakdown for an assessment session")
    public List<ScoreBreakdown> getScoreBreakdown(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = isSuperAdmin(jwt) ? null : extractTenantId(jwt);
        return runAssessmentUseCase.getScoreBreakdown(tenantId, sessionId);
    }

    @SecuredEndpoint(obj = "risk.assessment", act = "manage")
    @PostMapping("/{sessionId}/re-evaluate")
    @Operation(summary = "Re-evaluate a scored assessment session")
    public AssessmentSession reEvaluate(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID actorId = UUID.fromString(jwt.getSubject());
        log.info("Re-evaluating session: {} tenant: {}", sessionId, tenantId);
        return runAssessmentUseCase.reEvaluate(tenantId, sessionId, actorId);
    }

    // ===== REQUEST RECORDS =====

    public record InitiateRequest(
            String riskType,
            String entityReference,
            String idempotencyKey
    ) {}

    public record SubmitAnswersRequest(
            List<AnswerItem> answers
    ) {}

    public record AnswerItem(
            UUID parameterId,
            String answerValue,
            String languageCode
    ) {}

    @SuppressWarnings("unchecked")
    private boolean isSuperAdmin(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        var roles = (java.util.Collection<String>) realmAccess.get("roles");
        return roles != null && roles.contains("super_admin");
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
