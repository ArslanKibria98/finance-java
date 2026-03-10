package com.ksa.financing.risk.application.state;

import com.ksa.financing.risk.domain.model.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable orchestration state for the internal checks pipeline.
 * Lives in the application layer because it tracks process progress,
 * not domain invariants.
 */
@Data
public class InternalCheckState {
    private InternalCheckStep currentStep;
    private String assessmentId;
    private String nidHash;
    private String mobileHash;
    private String deviceId;
    private CheckDecision overallDecision;
    private int accumulatedRiskScore;
    private RiskLevel riskLevel;
    private Set<String> flags = new HashSet<>();
    private String blockReason;
    private String routeTo;
    private List<CheckStepResult> completedChecks = new ArrayList<>();
    private String failureReason;
    private CifStatus cifStatus;
    private Instant startedAt;
    private Instant lastUpdatedAt;
}
