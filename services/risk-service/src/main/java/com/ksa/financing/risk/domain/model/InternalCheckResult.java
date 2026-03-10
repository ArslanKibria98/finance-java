package com.ksa.financing.risk.domain.model;

import java.util.List;
import java.util.Set;

public record InternalCheckResult(
    String assessmentId,
    RiskAssessmentStatus status,
    CheckDecision overallDecision,
    int riskScore,
    RiskLevel riskLevel,
    Set<String> flags,
    String blockReason,
    String routeTo,
    List<CheckStepResult> stepResults,
    String failureReason
) {}
