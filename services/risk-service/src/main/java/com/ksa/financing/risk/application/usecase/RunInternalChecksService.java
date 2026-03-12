package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.application.state.InternalCheckState;
import com.ksa.financing.risk.domain.model.*;
import com.ksa.financing.risk.domain.port.in.RunInternalChecksUseCase;
import com.ksa.financing.risk.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunInternalChecksService implements RunInternalChecksUseCase {

    private final NidFormatValidator nidFormatValidator;
    private final BlacklistWatchlistCheck blacklistWatchlistCheck;
    private final FraudHistoryCheck fraudHistoryCheck;
    private final DeviceFingerprintCheck deviceFingerprintCheck;
    private final AccountLockCheck accountLockCheck;
    private final InternalSanctionsCheck internalSanctionsCheck;
    private final RiskScoreCalculator riskScoreCalculator;
    private final CifLookupCheck cifLookupCheck;
    private final DuplicateMobileCheck duplicateMobileCheck;

    @Override
    public InternalCheckResult run(InternalCheckRequest request) {
        String assessmentId = UUID.randomUUID().toString();
        log.info("Starting internal checks assessment: {}", assessmentId);

        var state = new InternalCheckState();
        state.setAssessmentId(assessmentId);
        state.setNidHash(request.nidHash());
        state.setMobileHash(request.mobileHash());
        state.setDeviceId(request.deviceId());
        state.setCurrentStep(InternalCheckStep.INITIATED);
        state.setStartedAt(Instant.now());
        state.setLastUpdatedAt(Instant.now());
        state.setFlags(new HashSet<>());
        state.setCompletedChecks(new ArrayList<>());

        // Scoring factors for Risk Score check
        boolean watchlistMatch = false;
        boolean newDevice = true;
        boolean newNid = true;
        boolean newMobile = true;
        int deviceNidCount = 0;

        try {
            // =================================================================
            // CHECK 1: NID Format Validation
            // =================================================================
            log.info("Check 1: NID Format Validation for assessment: {}", assessmentId);

            var nidResult = nidFormatValidator.validate(
                new NidFormatValidator.NidValidationInput(request.nationalId())
            );
            if (!nidResult.valid()) {
                return blockResult(assessmentId, state, nidResult.failureReason());
            }
            addCheckResult(state, "NID_FORMAT", CheckDecision.PASS, nidResult.idType(), 0);
            updateState(state, InternalCheckStep.NID_VALIDATED);

            // =================================================================
            // CHECK 2: Internal Blacklist / Watchlist
            // =================================================================
            log.info("Check 2: Blacklist/Watchlist Check for assessment: {}", assessmentId);

            var blacklistResult = blacklistWatchlistCheck.check(
                new BlacklistWatchlistCheck.BlacklistInput(
                    request.nationalId(), request.mobileNumber(),
                    request.nidHash(), request.mobileHash()
                )
            );
            if (blacklistResult.decision() == CheckDecision.HARD_BLOCK) {
                return silentBlock(assessmentId, state);
            }
            if (blacklistResult.watchlisted()) {
                state.getFlags().add("ENHANCED_MONITORING");
                watchlistMatch = true;
            }
            addCheckResult(state, "BLACKLIST_WATCHLIST", blacklistResult.decision(),
                blacklistResult.matchType(), blacklistResult.watchlisted() ? 40 : 0);
            updateState(state, InternalCheckStep.BLACKLIST_CHECKED);

            // =================================================================
            // CHECK 3: Fraud History Flags
            // =================================================================
            log.info("Check 3: Fraud History Check for assessment: {}", assessmentId);

            var fraudResult = fraudHistoryCheck.check(
                new FraudHistoryCheck.FraudHistoryInput(
                    request.nidHash(), request.mobileHash(), null
                )
            );
            if (fraudResult.decision() == CheckDecision.HARD_BLOCK) {
                return silentBlock(assessmentId, state);
            }
            if (fraudResult.suspectedFraud()) {
                state.getFlags().add("HIGH_RISK");
            }
            addCheckResult(state, "FRAUD_HISTORY", fraudResult.decision(),
                "incidents=" + fraudResult.incidentCount(), 0);
            updateState(state, InternalCheckStep.FRAUD_CHECKED);

            // =================================================================
            // CHECK 4: Device Fingerprint
            // =================================================================
            log.info("Check 4: Device Fingerprint Check for assessment: {}", assessmentId);

            var deviceResult = deviceFingerprintCheck.check(
                new DeviceFingerprintCheck.DeviceFingerprintInput(
                    request.deviceId(), request.deviceFingerprint()
                )
            );
            if (deviceResult.decision() == CheckDecision.HARD_BLOCK) {
                return blockResult(assessmentId, state, deviceResult.failureReason());
            }
            if (deviceResult.decision() == CheckDecision.SOFT_BLOCK) {
                return blockResult(assessmentId, state, deviceResult.failureReason());
            }
            deviceNidCount = deviceResult.nidAssociationCount();
            newDevice = deviceResult.nidAssociationCount() == 0;
            addCheckResult(state, "DEVICE_FINGERPRINT", CheckDecision.PASS,
                "nidCount=" + deviceResult.nidAssociationCount(), 0);
            updateState(state, InternalCheckStep.DEVICE_CHECKED);

            // =================================================================
            // CHECK 5: Account Lock History
            // =================================================================
            log.info("Check 5: Account Lock Check for assessment: {}", assessmentId);

            var lockResult = accountLockCheck.check(
                new AccountLockCheck.AccountLockInput(request.nidHash())
            );
            if (lockResult.decision() == CheckDecision.HARD_BLOCK) {
                return blockResult(assessmentId, state, lockResult.failureReason());
            }
            if (lockResult.decision() == CheckDecision.ROUTE_REONBOARDING) {
                state.setRouteTo("REONBOARDING");
            }
            addCheckResult(state, "ACCOUNT_LOCK", lockResult.decision(),
                lockResult.lockType(), 0);
            updateState(state, InternalCheckStep.ACCOUNT_LOCK_CHECKED);

            // =================================================================
            // CHECK 6: Internal Sanctions List
            // =================================================================
            log.info("Check 6: Internal Sanctions Check for assessment: {}", assessmentId);

            var sanctionsResult = internalSanctionsCheck.check(
                new InternalSanctionsCheck.InternalSanctionsInput(request.nidHash(), request.tenantId())
            );
            if (sanctionsResult.sanctionsMatch()) {
                return silentBlock(assessmentId, state);
            }
            addCheckResult(state, "INTERNAL_SANCTIONS", CheckDecision.PASS, "No match", 0);
            updateState(state, InternalCheckStep.SANCTIONS_CHECKED);

            // =================================================================
            // CHECK 7: Preliminary Internal Risk Score
            // =================================================================
            log.info("Check 7: Risk Score Calculation for assessment: {}", assessmentId);

            var scoreResult = riskScoreCalculator.calculateScore(
                new RiskScoreCalculator.RiskScoreInput(
                    state.getCompletedChecks(),
                    watchlistMatch,
                    false,
                    newDevice,
                    newNid,
                    newMobile,
                    deviceNidCount
                )
            );

            if (scoreResult.decision() == CheckDecision.HARD_BLOCK) {
                state.setAccumulatedRiskScore(scoreResult.totalScore());
                state.setRiskLevel(scoreResult.riskLevel());
                return blockResult(assessmentId, state, "Unable to proceed at this time.");
            }
            if (scoreResult.riskLevel() == RiskLevel.HIGH) {
                state.getFlags().add("ENHANCED_MONITORING");
            }
            state.setAccumulatedRiskScore(scoreResult.totalScore());
            state.setRiskLevel(scoreResult.riskLevel());
            addCheckResult(state, "RISK_SCORE", scoreResult.decision(),
                "score=" + scoreResult.totalScore() + " level=" + scoreResult.riskLevel(), 0);
            updateState(state, InternalCheckStep.RISK_SCORED);

            // =================================================================
            // CHECK 8: CIF/NID Lookup (existing customer check)
            // =================================================================
            log.info("Check 8: CIF/NID Lookup for assessment: {}", assessmentId);

            var cifResult = cifLookupCheck.lookup(
                new CifLookupCheck.CifLookupInput(request.nidHash())
            );

            state.setCifStatus(cifResult.status());

            switch (cifResult.status()) {
                case BLOCKED -> {
                    // Do NOT reveal reason — silent block
                    addCheckResult(state, "CIF_LOOKUP", CheckDecision.HARD_BLOCK, "BLOCKED", 0);
                    updateState(state, InternalCheckStep.CIF_CHECKED);
                    return silentBlock(assessmentId, state);
                }
                case EXISTING_ACTIVE -> {
                    // Fully onboarded — route to Login
                    addCheckResult(state, "CIF_LOOKUP", CheckDecision.ROUTE_LOGIN,
                        "An account already exists with this ID. Please log in. Forgot Password?", 0);
                    updateState(state, InternalCheckStep.CIF_CHECKED);
                    return alreadyExistsResult(assessmentId, state,
                        "An account already exists with this ID. Please log in. Forgot Password?");
                }
                case EXISTING_PENDING -> {
                    // Onboarding started but not completed — route to Login
                    addCheckResult(state, "CIF_LOOKUP", CheckDecision.ROUTE_LOGIN,
                        "An account already exists with this ID. Please log in. Forgot Password?", 0);
                    updateState(state, InternalCheckStep.CIF_CHECKED);
                    return alreadyExistsResult(assessmentId, state,
                        "An account already exists with this ID. Please log in. Forgot Password?");
                }
                case EXISTING_INACTIVE, EXISTING_DORMANT -> {
                    // Route to Welcome Back reactivation flow
                    addCheckResult(state, "CIF_LOOKUP", CheckDecision.ROUTE_REONBOARDING,
                        cifResult.status().name(), 0);
                    updateState(state, InternalCheckStep.CIF_CHECKED);
                    return welcomeBackResult(assessmentId, state);
                }
                default -> {
                    // NEW customer — proceed
                    newNid = true;
                    addCheckResult(state, "CIF_LOOKUP", CheckDecision.PASS, "NEW", 0);
                    updateState(state, InternalCheckStep.CIF_CHECKED);
                }
            }

            // =================================================================
            // CHECK 9: Duplicate Mobile Number
            // =================================================================
            log.info("Check 9: Duplicate Mobile Check for assessment: {}", assessmentId);

            var mobileResult = duplicateMobileCheck.check(
                new DuplicateMobileCheck.DuplicateMobileInput(
                    request.mobileHash(), request.sessionId()
                )
            );
            if (mobileResult.duplicate()) {
                addCheckResult(state, "DUPLICATE_MOBILE", CheckDecision.ROUTE_LOGIN,
                    "This mobile number is already registered. Please log in or use a different number.", 0);
                updateState(state, InternalCheckStep.MOBILE_CHECKED);
                return alreadyExistsResult(assessmentId, state,
                    "This mobile number is already registered. Please log in or use a different number.");
            }
            newMobile = true;
            addCheckResult(state, "DUPLICATE_MOBILE", CheckDecision.PASS, "Mobile not registered", 0);
            updateState(state, InternalCheckStep.MOBILE_CHECKED);

            // =================================================================
            // SUCCESS - All 9 checks passed
            // =================================================================
            state.setOverallDecision(CheckDecision.PASS);
            updateState(state, InternalCheckStep.COMPLETED);
            log.info("Internal Checks COMPLETED for assessment: {} riskScore={} riskLevel={} flags={}",
                assessmentId, state.getAccumulatedRiskScore(), state.getRiskLevel(), state.getFlags());

            return new InternalCheckResult(
                assessmentId,
                RiskAssessmentStatus.COMPLETED,
                CheckDecision.PASS,
                state.getAccumulatedRiskScore(),
                state.getRiskLevel(),
                state.getFlags(),
                null,
                state.getRouteTo(),
                state.getCompletedChecks(),
                null
            );

        } catch (Exception e) {
            log.error("Internal Checks FAILED for assessment: {}", assessmentId, e);
            return fail(assessmentId, state, "Unexpected error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private void updateState(InternalCheckState state, InternalCheckStep step) {
        state.setCurrentStep(step);
        state.setLastUpdatedAt(Instant.now());
    }

    private void addCheckResult(InternalCheckState state, String checkName,
                                CheckDecision decision, String detail, int scoreContribution) {
        state.getCompletedChecks().add(new CheckStepResult(
            checkName, decision, detail, scoreContribution, Instant.now()
        ));
    }

    private InternalCheckResult alreadyExistsResult(String assessmentId, InternalCheckState state, String message) {
        state.setOverallDecision(CheckDecision.ROUTE_LOGIN);
        state.setRouteTo("LOGIN");
        updateState(state, InternalCheckStep.COMPLETED);
        log.info("Already exists for assessment {}: {}", assessmentId, message);

        return new InternalCheckResult(
            assessmentId,
            RiskAssessmentStatus.COMPLETED,
            CheckDecision.ROUTE_LOGIN,
            state.getAccumulatedRiskScore(),
            state.getRiskLevel(),
            state.getFlags(),
            message,
            "LOGIN",
            state.getCompletedChecks(),
            null
        );
    }

    private InternalCheckResult welcomeBackResult(String assessmentId, InternalCheckState state) {
        state.setOverallDecision(CheckDecision.ROUTE_REONBOARDING);
        state.setRouteTo("WELCOME_BACK");
        updateState(state, InternalCheckStep.COMPLETED);
        log.info("Welcome back flow for assessment {}: customer is {}", assessmentId, state.getCifStatus());

        return new InternalCheckResult(
            assessmentId,
            RiskAssessmentStatus.COMPLETED,
            CheckDecision.ROUTE_REONBOARDING,
            state.getAccumulatedRiskScore(),
            state.getRiskLevel(),
            state.getFlags(),
            "Welcome back! Your account is inactive. Please reactivate.",
            "WELCOME_BACK",
            state.getCompletedChecks(),
            null
        );
    }

    private InternalCheckResult silentBlock(String assessmentId, InternalCheckState state) {
        state.setOverallDecision(CheckDecision.HARD_BLOCK);
        state.setBlockReason("Unable to proceed with registration at this time.");
        updateState(state, InternalCheckStep.BLOCKED);
        log.warn("Silent hard block for assessment: {}", assessmentId);

        return new InternalCheckResult(
            assessmentId,
            RiskAssessmentStatus.BLOCKED,
            CheckDecision.HARD_BLOCK,
            state.getAccumulatedRiskScore(),
            state.getRiskLevel(),
            state.getFlags(),
            "Unable to proceed with registration at this time.",
            null,
            state.getCompletedChecks(),
            null
        );
    }

    private InternalCheckResult blockResult(String assessmentId, InternalCheckState state, String reason) {
        state.setOverallDecision(CheckDecision.HARD_BLOCK);
        state.setBlockReason(reason);
        updateState(state, InternalCheckStep.BLOCKED);
        log.warn("Blocked for assessment {}: {}", assessmentId, reason);

        return new InternalCheckResult(
            assessmentId,
            RiskAssessmentStatus.BLOCKED,
            CheckDecision.HARD_BLOCK,
            state.getAccumulatedRiskScore(),
            state.getRiskLevel(),
            state.getFlags(),
            reason,
            null,
            state.getCompletedChecks(),
            null
        );
    }

    private InternalCheckResult fail(String assessmentId, InternalCheckState state, String reason) {
        state.setFailureReason(reason);
        updateState(state, InternalCheckStep.FAILED);
        log.error("Assessment failed {}: {}", assessmentId, reason);

        return new InternalCheckResult(
            assessmentId,
            RiskAssessmentStatus.FAILED,
            null,
            state.getAccumulatedRiskScore(),
            state.getRiskLevel(),
            state.getFlags(),
            null,
            null,
            state.getCompletedChecks(),
            reason
        );
    }
}
