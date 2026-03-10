package com.ksa.islamic.orchestration.activity.risk.dto;

import java.time.Instant;

public record CheckStepResult(
    String checkName,
    CheckDecision decision,
    String detail,
    int scoreContribution,
    Instant completedAt
) {}
