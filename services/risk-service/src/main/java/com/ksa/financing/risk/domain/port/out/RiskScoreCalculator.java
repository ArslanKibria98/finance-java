package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.model.CheckStepResult;
import com.ksa.financing.risk.domain.model.RiskLevel;

import java.util.List;

public interface RiskScoreCalculator {

    RiskScoreResult calculateScore(RiskScoreInput input);

    record RiskScoreInput(List<CheckStepResult> previousCheckResults, boolean watchlistMatch,
                          boolean velocityAnomaly, boolean newDevice, boolean newNid,
                          boolean newMobile, int deviceNidCount) {}

    record RiskScoreResult(int totalScore, RiskLevel riskLevel, CheckDecision decision,
                           List<String> contributingFactors, String failureReason) {}
}
