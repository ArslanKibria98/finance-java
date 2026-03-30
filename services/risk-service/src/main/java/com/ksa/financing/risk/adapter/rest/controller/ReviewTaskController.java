package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.review.ReviewAction;
import com.ksa.financing.risk.domain.model.review.ReviewRecommendation;
import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;
import com.ksa.financing.risk.domain.port.in.ManageReviewTaskUseCase;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review Tasks", description = "APIs for managing maker-checker review workflow")
public class ReviewTaskController {

    private final ManageReviewTaskUseCase manageReviewTaskUseCase;

    @SecuredEndpoint(obj = "risk.reviews", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get a review task by ID")
    public ReviewTask getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageReviewTaskUseCase.getById(tenantId, id);
    }

    @SecuredEndpoint(obj = "risk.reviews", act = "read")
    @GetMapping("/pending/maker")
    @Operation(summary = "Get all pending maker tasks")
    public List<ReviewTask> getPendingMakerTasks(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageReviewTaskUseCase.getPendingMakerTasks(tenantId);
    }

    @SecuredEndpoint(obj = "risk.reviews", act = "read")
    @GetMapping("/pending/approver")
    @Operation(summary = "Get all pending approver tasks")
    public List<ReviewTask> getPendingApproverTasks(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageReviewTaskUseCase.getPendingApproverTasks(tenantId);
    }

    @SecuredEndpoint(obj = "risk.reviews", act = "read")
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get review tasks by assessment session")
    public List<ReviewTask> getBySession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageReviewTaskUseCase.getBySession(tenantId, sessionId);
    }

    @SecuredEndpoint(obj = "risk.reviews", act = "manage")
    @PostMapping("/{id}/maker-recommend")
    @Operation(summary = "Submit maker recommendation for a review task")
    public ReviewTask makerRecommend(
            @PathVariable UUID id,
            @Valid @RequestBody MakerRecommendRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID makerId = UUID.fromString(jwt.getSubject());
        log.info("Maker recommendation for task: {} by: {} tenant: {}", id, makerId, tenantId);
        var command = new ManageReviewTaskUseCase.MakerRecommendCommand(
                makerId,
                ReviewRecommendation.valueOf(request.recommendation()),
                request.comment(),
                request.accountStatus() != null
                        ? AccountStatus.valueOf(request.accountStatus()) : null,
                request.complianceStatus() != null
                        ? ComplianceStatus.valueOf(request.complianceStatus()) : null
        );
        return manageReviewTaskUseCase.makerRecommend(tenantId, id, command);
    }

    @SecuredEndpoint(obj = "risk.reviews", act = "manage")
    @PostMapping("/{id}/approver-decide")
    @Operation(summary = "Submit approver decision for a review task")
    public ReviewTask approverDecide(
            @PathVariable UUID id,
            @Valid @RequestBody ApproverDecideRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID approverId = UUID.fromString(jwt.getSubject());
        log.info("Approver decision for task: {} by: {} tenant: {}", id, approverId, tenantId);
        var command = new ManageReviewTaskUseCase.ApproverDecideCommand(
                approverId,
                ReviewAction.valueOf(request.action()),
                request.comment(),
                request.accountStatus() != null
                        ? AccountStatus.valueOf(request.accountStatus()) : null,
                request.complianceStatus() != null
                        ? ComplianceStatus.valueOf(request.complianceStatus()) : null
        );
        return manageReviewTaskUseCase.approverDecide(tenantId, id, command);
    }

    @SecuredEndpoint(obj = "risk.reviews", act = "read")
    @GetMapping("/sla-breached")
    @Operation(summary = "Get all SLA-breached review tasks")
    public List<ReviewTask> getSlaBreachedTasks(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageReviewTaskUseCase.getSlaBreachedTasks(tenantId);
    }

    // ===== REQUEST RECORDS =====

    public record MakerRecommendRequest(
            String recommendation,
            String comment,
            String accountStatus,
            String complianceStatus
    ) {}

    public record ApproverDecideRequest(
            String action,
            String comment,
            String accountStatus,
            String complianceStatus
    ) {}

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
