package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.approval.ApprovalEvaluationResult;
import com.ksa.financing.risk.domain.port.in.EvaluateApprovalWorkflowUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/approval-workflows")
@RequiredArgsConstructor
public class ApprovalWorkflowController {

    private final EvaluateApprovalWorkflowUseCase evaluateApprovalWorkflowUseCase;

    @PostMapping("/products/{productId}/evaluate")
    public ResponseEntity<Map<String, Object>> evaluate(
            @PathVariable UUID productId,
            @RequestBody Map<String, String> applicationData,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));

        ApprovalEvaluationResult result = evaluateApprovalWorkflowUseCase.evaluate(
                tenantId, productId, applicationData, jwt.getTokenValue());

        return ResponseEntity.ok(Map.of(
                "data", result,
                "message", "success",
                "timestamp", java.time.Instant.now().toString()));
    }
}
