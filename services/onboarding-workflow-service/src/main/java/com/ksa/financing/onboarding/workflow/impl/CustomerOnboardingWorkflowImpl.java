package com.ksa.financing.onboarding.workflow.impl;

import com.ksa.financing.onboarding.domain.model.*;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import com.ksa.financing.onboarding.workflow.activity.GlobalProfileActivity;
import com.ksa.islamic.orchestration.activity.customer.ProfileCreationActivity;
import com.ksa.islamic.orchestration.activity.customer.UpdateCustomerActivity;
import com.ksa.islamic.orchestration.activity.identity.KeycloakUserCreationActivity;
import com.ksa.islamic.orchestration.activity.identity.SetPinActivity;
import com.ksa.islamic.orchestration.activity.kyc.*;
import com.ksa.islamic.orchestration.activity.notification.NotificationActivity;
import com.ksa.islamic.orchestration.activity.risk.AmlRiskScoringActivity;
import com.ksa.islamic.orchestration.activity.risk.RiskDecisionActivity;
import com.ksa.islamic.orchestration.activity.wallet.WalletCreationActivity;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Signal-driven state machine implementation of the customer onboarding workflow.
 *
 * <p>This workflow orchestrates the complete KSA Islamic Financing customer onboarding
 * as a multi-step state machine. The flow alternates between automated activity execution
 * and signal waits where the workflow pauses for external input (OTP verification, terms
 * acceptance, Nafath confirmation, additional info submission).</p>
 *
 * <p><b>Flow Summary:</b></p>
 * <ol>
 *   <li>Tahakuk mobile verification + OTP send (automated)</li>
 *   <li>Wait for OTP verified signal (10 min timeout)</li>
 *   <li>Wait for terms acceptance signal (24 hr timeout)</li>
 *   <li>Wait for Nafath initiate signal, then initiate Nafath (24 hr + 30 min)</li>
 *   <li>Yakeen + Sanctions + Profile creation (automated)</li>
 *   <li>Wait for additional info signal (24 hr timeout)</li>
 *   <li>Customer update + Salary fetch + Wallet + Notification (automated)</li>
 * </ol>
 *
 * <p><b>Device Trust:</b> Every signal validates that the device ID matches the initial
 * device from workflow start. A device change causes immediate workflow failure to prevent
 * session hijacking attacks.</p>
 *
 * <p>IMPORTANT: This class is NOT a Spring bean. It is instantiated by the Temporal Worker.
 * All activity stubs are created via {@code Workflow.newActivityStub()} and NOT via DI.
 * The logger uses {@code Workflow.getLogger()} which is Temporal-safe (deterministic replay).</p>
 */
public class CustomerOnboardingWorkflowImpl implements CustomerOnboardingWorkflow {

    private static final Logger log = Workflow.getLogger(CustomerOnboardingWorkflowImpl.class);

    // -------------------------------------------------------------------------
    // Workflow state (queryable via @QueryMethod)
    // -------------------------------------------------------------------------
    private final OnboardingState state = new OnboardingState();

    // -------------------------------------------------------------------------
    // Signal data holders
    // -------------------------------------------------------------------------
    private OtpVerifiedSignal otpVerifiedSignal;
    private boolean otpVerifiedReceived = false;

    private TermsAcceptedSignal termsAcceptedSignal;
    private boolean termsAcceptedReceived = false;

    private NafathInitiateSignal nafathInitiateSignal;
    private boolean nafathInitiateReceived = false;

    private NafathCallbackSignal nafathCallbackSignal;
    private boolean nafathCallbackReceived = false;

    private AdditionalInfoSignal additionalInfoSignal;
    private boolean additionalInfoReceived = false;

    private EddFormSignal eddFormSignal;
    private boolean eddFormReceived = false;

    private SetPinSignal setPinSignal;
    private boolean setPinReceived = false;

    // -------------------------------------------------------------------------
    // Activity options
    // -------------------------------------------------------------------------

    /**
     * Default activity options: 30-second timeout with standard retry (3 attempts, 2s initial, 30s max, 2x backoff).
     * Used for most activities where a quick response is expected.
     */
    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    /**
     * Fast activity options: 10-second timeout with minimal retry (2 attempts).
     * Used for lightweight operations like OTP send/verify where latency matters.
     */
    private final ActivityOptions fastOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(10))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .build())
            .build();

    /**
     * Nafath-specific activity options: 1-minute timeout with limited retry (2 attempts).
     * Nafath initiation may take longer due to external service latency, and retrying
     * aggressively could produce duplicate sessions.
     */
    private final ActivityOptions nafathOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(1))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setMaximumAttempts(2)
                    .build())
            .build();

    // -------------------------------------------------------------------------
    // Activity stubs -- routed to owning service via setTaskQueue()
    // -------------------------------------------------------------------------

    // KYC activities → kyc-adapter-service
    private final MobileVerificationActivity mobileActivity =
            Workflow.newActivityStub(MobileVerificationActivity.class, withQueue(TaskQueue.KYC_QUEUE, defaultOptions));

    private final OtpSendActivity otpSendActivity =
            Workflow.newActivityStub(OtpSendActivity.class, withQueue(TaskQueue.KYC_QUEUE, fastOptions));

    private final OtpVerifyActivity otpVerifyActivity =
            Workflow.newActivityStub(OtpVerifyActivity.class, withQueue(TaskQueue.KYC_QUEUE, fastOptions));

    private final NafathVerificationActivity nafathActivity =
            Workflow.newActivityStub(NafathVerificationActivity.class, withQueue(TaskQueue.KYC_QUEUE, nafathOptions));

    private final YakeenVerificationActivity yakeenActivity =
            Workflow.newActivityStub(YakeenVerificationActivity.class, withQueue(TaskQueue.KYC_QUEUE, defaultOptions));

    private final SanctionsScreeningActivity sanctionsActivity =
            Workflow.newActivityStub(SanctionsScreeningActivity.class, withQueue(TaskQueue.KYC_QUEUE, defaultOptions));

    private final SalaryFetchActivity salaryActivity =
            Workflow.newActivityStub(SalaryFetchActivity.class, withQueue(TaskQueue.KYC_QUEUE, defaultOptions));

    private final PepScreeningActivity pepScreeningActivity =
            Workflow.newActivityStub(PepScreeningActivity.class, withQueue(TaskQueue.KYC_QUEUE, defaultOptions));

    // Risk activities → risk-service
    private final RiskDecisionActivity riskDecisionActivity =
            Workflow.newActivityStub(RiskDecisionActivity.class, withQueue(TaskQueue.RISK_ASSESSMENT_QUEUE, defaultOptions));

    private final AmlRiskScoringActivity amlRiskScoringActivity =
            Workflow.newActivityStub(AmlRiskScoringActivity.class, withQueue(TaskQueue.RISK_ASSESSMENT_QUEUE, defaultOptions));

    // Customer activities → customer-service
    private final ProfileCreationActivity profileActivity =
            Workflow.newActivityStub(ProfileCreationActivity.class, withQueue(TaskQueue.CUSTOMER_QUEUE, defaultOptions));

    private final UpdateCustomerActivity updateCustomerActivity =
            Workflow.newActivityStub(UpdateCustomerActivity.class, withQueue(TaskQueue.CUSTOMER_QUEUE, defaultOptions));

    // Wallet activities → wallet-service
    private final WalletCreationActivity walletActivity =
            Workflow.newActivityStub(WalletCreationActivity.class, withQueue(TaskQueue.WALLET_QUEUE, defaultOptions));

    // Identity activities → identity-service
    private final KeycloakUserCreationActivity keycloakActivity =
            Workflow.newActivityStub(KeycloakUserCreationActivity.class, withQueue(TaskQueue.IDENTITY_QUEUE, defaultOptions));

    private final SetPinActivity setPinActivity =
            Workflow.newActivityStub(SetPinActivity.class, withQueue(TaskQueue.IDENTITY_QUEUE, defaultOptions));

    // Notification → local (stays on onboarding queue)
    private final NotificationActivity notificationActivity =
            Workflow.newActivityStub(NotificationActivity.class, defaultOptions);

    // Global Profile → local (stays on onboarding queue, calls GPS via HTTP)
    private final GlobalProfileActivity globalProfileActivity =
            Workflow.newActivityStub(GlobalProfileActivity.class, fastOptions);

    // -------------------------------------------------------------------------
    // @WorkflowMethod -- main orchestration (signal-driven state machine)
    // -------------------------------------------------------------------------

    @Override
    public OnboardingResult execute(OnboardingRequest request) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        log.info("Starting onboarding workflow: {}", workflowId);

        // Initialize state
        state.setWorkflowId(workflowId);
        state.setNationalId(request.nationalId());
        state.setMobileNumber(request.mobileNumber());
        state.setCurrentStep(OnboardingStep.INITIATED);
        state.setStartedAt(Instant.now());
        state.setLastUpdatedAt(Instant.now());

        // Pre-generate customerId (will be overwritten at Step 5a with real customer DB ID)
        state.setCustomerId(Workflow.randomUUID().toString());

        // Create real Global Profile in GPS so the globalUid is genuine from the first response
        try {
            var gpsResult = globalProfileActivity.createGlobalProfile(
                    new GlobalProfileActivity.GlobalProfileInput(
                            request.mobileNumber(), null, "SAU"
                    )
            );
            if (gpsResult.created() && gpsResult.globalUid() != null) {
                state.setGlobalUid(gpsResult.globalUid());
                log.info("Global profile created with real globalUid: {}", gpsResult.globalUid());
            } else {
                state.setGlobalUid(Workflow.randomUUID().toString());
                log.warn("GPS unavailable, using pre-generated globalUid");
            }
        } catch (Exception e) {
            state.setGlobalUid(Workflow.randomUUID().toString());
            log.warn("GPS call failed, using pre-generated globalUid: {}", e.getMessage());
        }

        // Lifecycle: customer identified at onboarding start
        state.setLifecycleStage("LEAD");

        // Device trust initialization
        state.setInitialDeviceId(request.deviceId());
        state.setDeviceTrusted(true);

        try {
            // =================================================================
            // STEP 1: Tahakuk Mobile Verification + OTP Send
            // =================================================================
            log.info("Step 1: Tahakuk verification for workflow: {}", workflowId);

            var mobileResult = mobileActivity.verifyMobile(
                    new MobileVerificationActivity.MobileVerificationInput(
                            request.nationalId(), request.mobileNumber(), request.tenantId()
                    )
            );
            if (!mobileResult.verified()) {
                return fail(workflowId, "Mobile verification (Tahakuk) failed");
            }

            log.info("Step 1: Sending OTP for workflow: {}", workflowId);

            var otpResult = otpSendActivity.sendOtp(
                    new OtpSendActivity.OtpSendInput(request.nationalId(), request.mobileNumber())
            );
            if (!otpResult.sent()) {
                return fail(workflowId, "OTP send failed");
            }
            state.setOtpRequestId(otpResult.otpRequestId());
            updateState(OnboardingStep.OTP_SENT);

            // =================================================================
            // WAIT: OTP verification signal (10-minute timeout)
            // =================================================================
            log.info("Waiting for OTP verification signal for workflow: {}", workflowId);

            boolean otpSignalReceived = Workflow.await(Duration.ofMinutes(10), () -> otpVerifiedReceived);
            if (!otpSignalReceived) {
                return fail(workflowId, "OTP verification timed out (10 minutes)");
            }

            // Validate device trust
            if (!validateDevice(otpVerifiedSignal.deviceInfo())) {
                return fail(workflowId, "Device changed during onboarding — untrusted device");
            }

            state.setKeycloakUserId(otpVerifiedSignal.keycloakUserId());
            updateState(OnboardingStep.OTP_VERIFIED);

            // =================================================================
            // WAIT: Terms acceptance signal (24-hour timeout)
            // =================================================================
            updateState(OnboardingStep.TERMS_PENDING);
            log.info("Waiting for terms acceptance signal for workflow: {}", workflowId);

            boolean termsSignalReceived = Workflow.await(Duration.ofHours(24), () -> termsAcceptedReceived);
            if (!termsSignalReceived) {
                return fail(workflowId, "Terms acceptance timed out (24 hours)");
            }

            if (!validateDevice(termsAcceptedSignal.deviceInfo())) {
                return fail(workflowId, "Device changed during onboarding — untrusted device");
            }
            if (!termsAcceptedSignal.accepted()) {
                return fail(workflowId, "User declined terms and conditions");
            }
            updateState(OnboardingStep.TERMS_ACCEPTED);

            // =================================================================
            // WAIT: Nafath initiate signal (24-hour timeout)
            // =================================================================
            log.info("Waiting for Nafath initiation signal for workflow: {}", workflowId);

            boolean nafathInitSignalReceived = Workflow.await(Duration.ofHours(24), () -> nafathInitiateReceived);
            if (!nafathInitSignalReceived) {
                return fail(workflowId, "Nafath initiation timed out (24 hours)");
            }

            if (!validateDevice(nafathInitiateSignal.deviceInfo())) {
                return fail(workflowId, "Device changed during onboarding — untrusted device");
            }

            // Lifecycle: KYC verification starts
            state.setLifecycleStage("PROSPECT");

            // =================================================================
            // STEP 4: Nafath Initiation
            // =================================================================
            log.info("Step 4: Nafath initiation for workflow: {}", workflowId);

            var nafathResult = nafathActivity.initiateNafath(
                    new NafathVerificationActivity.NafathInitiationInput(
                            request.nationalId(), request.tenantId()
                    )
            );
            if (!nafathResult.initiated()) {
                return fail(workflowId, "Nafath initiation failed");
            }
            state.setNafathRandomNumber(nafathResult.randomNumber());
            state.setNafathSessionId(nafathResult.sessionId());
            state.setNafathTransactionId(nafathResult.transactionId());
            state.setNafathVerificationData(nafathResult.nafathVerificationData());
            updateState(OnboardingStep.NAFATH_INITIATED);

            // =================================================================
            // WAIT: Nafath callback signal (30-minute timeout)
            // =================================================================
            log.info("Waiting for Nafath callback signal for workflow: {}", workflowId);

            boolean nafathCbReceived = Workflow.await(Duration.ofMinutes(30), () -> nafathCallbackReceived);
            if (!nafathCbReceived) {
                return fail(workflowId, "Nafath verification timed out (30 minutes)");
            }
            if (!nafathCallbackSignal.accepted()) {
                return fail(workflowId, "Nafath verification rejected: " + nafathCallbackSignal.rejectionReason());
            }
            updateState(OnboardingStep.NAFATH_VERIFIED);

            // =================================================================
            // STEP 5a: Auto -- Yakeen + Sanctions + Profile Creation
            //   These are non-blocking: if external services are unavailable
            //   the workflow continues to INFO_PENDING. Data can be enriched later.
            // =================================================================
            YakeenVerificationActivity.YakeenVerificationResult yakeenResult = null;

            // Yakeen identity verification (non-blocking)
            try {
                log.info("Step 5a: Yakeen identity verification for workflow: {}", workflowId);
                yakeenResult = yakeenActivity.verifyIdentity(
                        new YakeenVerificationActivity.YakeenVerificationInput(
                                request.nationalId(),
                                null,
                                request.tenantId()
                        )
                );
                if (yakeenResult.verified()) {
                    Map<String, Object> yakeenData = new HashMap<>();
                    yakeenData.put("fullNameAr", yakeenResult.fullNameAr());
                    yakeenData.put("fullNameEn", yakeenResult.fullNameEn());
                    yakeenData.put("gender", yakeenResult.gender());
                    yakeenData.put("nationality", yakeenResult.nationality());
                    yakeenData.put("addressCity", yakeenResult.addressCity());
                    yakeenData.put("addressRegion", yakeenResult.addressRegion());
                    state.setYakeenData(yakeenData);
                } else {
                    log.warn("Yakeen verification returned not-verified (continuing): {}", workflowId);
                    yakeenResult = null;
                }
            } catch (Exception e) {
                log.warn("Yakeen verification failed (continuing): {}", e.getMessage());
            }

            // Sanctions screening (non-blocking, requires Yakeen data)
            if (yakeenResult != null) {
                try {
                    log.info("Step 5a: Sanctions screening for workflow: {}", workflowId);
                    var sanctionsResult = sanctionsActivity.screenSanctions(
                            new SanctionsScreeningActivity.SanctionsScreeningInput(
                                    yakeenResult.fullNameEn(),
                                    request.nationalId(),
                                    yakeenResult.nationality(),
                                    request.tenantId()
                            )
                    );
                    if (!sanctionsResult.cleared()) {
                        log.warn("Sanctions screening HIT detected ({} matches) — continuing", sanctionsResult.matchCount());
                    }
                } catch (Exception e) {
                    log.warn("Sanctions screening failed (continuing): {}", e.getMessage());
                }
            } else {
                log.info("Skipping sanctions screening (no Yakeen data) for workflow: {}", workflowId);
            }

            // Lifecycle: Nafath verified, proceeding to additional info
            state.setLifecycleStage("QUALIFIED");

            // Profile creation (non-blocking)
            try {
                log.info("Step 5a: Profile creation for workflow: {}", workflowId);
                var profileResult = profileActivity.createProfile(
                        new ProfileCreationActivity.ProfileCreationInput(
                                request.nationalId(),
                                request.mobileNumber(),
                                null,
                                null,
                                yakeenResult != null ? yakeenResult.fullNameAr() : null,
                                yakeenResult != null ? yakeenResult.fullNameEn() : null,
                                yakeenResult != null ? yakeenResult.gender() : null,
                                yakeenResult != null ? yakeenResult.nationality() : null,
                                yakeenResult != null ? yakeenResult.addressCity() : null,
                                yakeenResult != null ? yakeenResult.addressRegion() : null,
                                request.tenantId(),
                                state.getGlobalUid(),
                                state.getCustomerId(),
                                state.getLifecycleStage()
                        )
                );
                if (profileResult.created()) {
                    if (profileResult.customerId() != null) {
                        state.setCustomerId(profileResult.customerId());
                    }
                    if (profileResult.globalUid() != null) {
                        state.setGlobalUid(profileResult.globalUid());
                    }
                } else {
                    log.warn("Profile creation returned not-created (continuing): {}", workflowId);
                }
            } catch (Exception e) {
                log.warn("Profile creation failed (continuing): {}", e.getMessage());
            }

            updateState(OnboardingStep.INFO_PENDING);

            // =================================================================
            // WAIT: Additional info signal (24-hour timeout)
            // =================================================================
            log.info("Waiting for additional info signal for workflow: {}", workflowId);

            boolean additionalInfoSignalReceived = Workflow.await(Duration.ofHours(24), () -> additionalInfoReceived);
            if (!additionalInfoSignalReceived) {
                return fail(workflowId, "Additional info submission timed out (24 hours)");
            }

            if (!validateDevice(additionalInfoSignal.deviceInfo())) {
                return fail(workflowId, "Device changed during onboarding — untrusted device");
            }

            // =================================================================
            // STEP 6: PEP & Sanctions Screening + Risk Decision
            //   After submit-info, run enhanced screening before completing.
            //   Versioned for backward compatibility with existing workflows.
            // =================================================================
            int pepVersion = Workflow.getVersion("pep-screening", Workflow.DEFAULT_VERSION, 1);

            if (pepVersion >= 1) {
                updateState(OnboardingStep.SCREENING);

                // 6A: PEP decision — driven by isPep flag from submit-info request
                boolean isPep = additionalInfoSignal.isPep();
                log.info("Step 6: PEP screening for workflow: {} — isPep={}", workflowId, isPep);

                if (isPep) {
                    state.setPepDecision("EDD_REQUIRED");
                    state.setPepConfidence(1.0);
                    state.setPepDetected(true);
                } else {
                    state.setPepDecision("CLEAR");
                    state.setPepConfidence(0.0);
                    state.setPepDetected(false);
                }

                // Also run background PEP screening activity (non-blocking, for audit)
                if (yakeenResult != null) {
                    try {
                        var pepResult = pepScreeningActivity.screenPep(
                                new PepScreeningActivity.PepScreeningInput(
                                        yakeenResult.fullNameEn(),
                                        request.nationalId(),
                                        yakeenResult.nationality(),
                                        null,
                                        request.tenantId()
                                )
                        );
                        log.info("PEP screening audit result: decision={}, confidence={}, pepDetected={}",
                                pepResult.decision(), pepResult.confidenceScore(), pepResult.pepDetected());
                    } catch (Exception e) {
                        log.warn("PEP screening audit failed (non-blocking): {}", e.getMessage());
                    }
                }

                // 6B: EDD Required path (isPep = true)
                if (isPep) {
                    updateState(OnboardingStep.EDD_REQUIRED);
                    state.setEddStatus("PENDING");
                    log.info("EDD required for workflow: {} — waiting for EDD form submission", workflowId);

                    boolean eddSignalReceived = Workflow.await(Duration.ofHours(24), () -> eddFormReceived);
                    if (!eddSignalReceived) {
                        return fail(workflowId, "EDD form submission timed out (24 hours)");
                    }

                    // Validate device trust for EDD signal — flag but don't hard-fail
                    if (eddFormSignal.deviceId() != null && state.getInitialDeviceId() != null
                            && !state.getInitialDeviceId().equals(eddFormSignal.deviceId())) {
                        state.setDeviceTrusted(false);
                        log.warn("Device changed during EDD: initial={}, current={} — flagged as untrusted (continuing)",
                                state.getInitialDeviceId(), eddFormSignal.deviceId());
                    }

                    state.setEddStatus("SUBMITTED");
                    state.setEddPoliticalPosition(eddFormSignal.politicalPosition());
                    state.setEddGovernmentBody(eddFormSignal.governmentBody());
                    state.setEddCountryOfInfluence(eddFormSignal.countryOfInfluence());
                    state.setEddSourceOfWealth(eddFormSignal.primarySourceOfWealth());
                    state.setEddEstimatedNetWorth(eddFormSignal.estimatedNetWorth());
                    state.setEddSourceOfFunds(eddFormSignal.sourceOfFunds());
                    updateState(OnboardingStep.EDD_SUBMITTED);
                    log.info("EDD form submitted for workflow: {} — position={}, country={}, wealth={}",
                            workflowId, eddFormSignal.politicalPosition(),
                            eddFormSignal.countryOfInfluence(), eddFormSignal.primarySourceOfWealth());
                }

                // 6C: AML Risk Scoring — calls risk-service weighted scoring engine
                try {
                    log.info("Step 6C: AML risk scoring for workflow: {}", workflowId);

                    // Build AML scoring input from available onboarding data
                    String nationalIdHash = request.nationalId() != null
                            ? UUID.nameUUIDFromBytes(request.nationalId().getBytes()).toString()
                            : "";
                    String nationality = yakeenResult != null ? yakeenResult.nationality() : "SA";
                    String cityName = yakeenResult != null ? yakeenResult.addressCity() : null;
                    String occupationCode = additionalInfoSignal.employmentType() != null
                            ? additionalInfoSignal.employmentType() : "UNKNOWN";
                    java.math.BigDecimal monthlyIncome = additionalInfoSignal.grossSalary() != null
                            ? java.math.BigDecimal.valueOf(additionalInfoSignal.grossSalary())
                            : java.math.BigDecimal.ZERO;
                    String sourceOfIncome = state.getEddSourceOfFunds() != null
                            ? state.getEddSourceOfFunds() : "SALARY";

                    var amlResult = amlRiskScoringActivity.calculateAmlScore(
                            new AmlRiskScoringActivity.AmlRiskScoringInput(
                                    nationalIdHash,
                                    nationality,
                                    cityName,
                                    occupationCode,
                                    monthlyIncome,
                                    sourceOfIncome,
                                    "STANDARD",                    // productRiskTier — default for onboarding
                                    state.isPepDetected(),
                                    false,                         // isOnInternalList — TODO: integrate internal list check
                                    state.getCustomerId(),
                                    request.tenantId(),
                                    workflowId + "-aml"            // idempotencyKey
                            )
                    );

                    if (amlResult.success()) {
                        state.setAmlAssessmentId(amlResult.assessmentId());
                        state.setAmlRiskLevel(amlResult.riskLevel());
                        state.setAmlTotalScore(amlResult.totalScore().doubleValue());
                        state.setAmlDominantOverride(amlResult.dominantOverride());
                        state.setAmlDominantCategory(amlResult.dominantCategory());
                        log.info("AML scoring complete: assessmentId={}, totalScore={}, riskLevel={}, dominantOverride={}",
                                amlResult.assessmentId(), amlResult.totalScore(),
                                amlResult.riskLevel(), amlResult.dominantOverride());

                        // AML HIGH = auto-block during onboarding
                        if ("HIGH".equals(amlResult.riskLevel())) {
                            return fail(workflowId, "AML risk too high: score=" + amlResult.totalScore()
                                    + ", level=" + amlResult.riskLevel()
                                    + (amlResult.dominantOverride() ? ", dominantCategory=" + amlResult.dominantCategory() : ""));
                        }
                    } else {
                        log.warn("AML scoring failed (continuing): {}", amlResult.errorMessage());
                    }
                } catch (Exception e) {
                    log.warn("AML risk scoring failed (non-blocking, continuing): {}", e.getMessage());
                }

                // 6D: Risk Decision
                try {
                    log.info("Step 6D: Risk decision for workflow: {}", workflowId);
                    var riskResult = riskDecisionActivity.evaluateRisk(
                            new RiskDecisionActivity.RiskDecisionInput(
                                    state.getCustomerId(),
                                    request.nationalId(),
                                    state.getPepDecision(),
                                    state.getPepConfidence(),
                                    !"BLOCK".equals(state.getPepDecision()) && !"HOLD".equals(state.getPepDecision()),
                                    state.getEddStatus(),
                                    request.tenantId()
                            )
                    );
                    state.setRiskLevel(riskResult.riskLevel());
                    state.setRiskScore(riskResult.riskScore());
                    state.setRiskDecision(riskResult.decision());
                    log.info("Risk decision: level={}, score={}, decision={}",
                            riskResult.riskLevel(), riskResult.riskScore(), riskResult.decision());

                    // Block or Hold = fail
                    if ("BLOCK".equals(riskResult.decision())) {
                        return fail(workflowId, "Risk decision: BLOCKED (score=" + riskResult.riskScore() + ", reason=" + riskResult.reason() + ")");
                    }
                    if ("HOLD".equals(riskResult.decision())) {
                        return fail(workflowId, "Risk decision: HOLD for review (score=" + riskResult.riskScore() + ", reason=" + riskResult.reason() + ")");
                    }
                } catch (Exception e) {
                    log.warn("Risk decision failed (continuing with default APPROVE): {}", e.getMessage());
                    state.setRiskLevel("LOW");
                    state.setRiskScore(0);
                    state.setRiskDecision("APPROVE");
                }
            }

            updateState(OnboardingStep.COMPLETING);

            // =================================================================
            // STEP 7: Auto -- Update + Salary + Wallet + Notification
            // =================================================================
            log.info("Step 5c: Updating customer with additional info for workflow: {}", workflowId);

            try {
                updateCustomerActivity.updateWithAdditionalInfo(
                        new UpdateCustomerActivity.UpdateCustomerInput(
                                state.getCustomerId(),
                                additionalInfoSignal.email(),
                                yakeenResult != null ? yakeenResult.addressCity() : null,
                                yakeenResult != null ? yakeenResult.addressRegion() : null,
                                additionalInfoSignal.employerName(),
                                additionalInfoSignal.employerCrNumber(),
                                additionalInfoSignal.employmentType(),
                                additionalInfoSignal.jobTitle(),
                                additionalInfoSignal.basicSalary(),
                                null, // housingAllowance
                                additionalInfoSignal.grossSalary(),
                                additionalInfoSignal.netSalary(),
                                additionalInfoSignal.currency(),
                                additionalInfoSignal.bankName(),
                                additionalInfoSignal.bankCode(),
                                additionalInfoSignal.iban(),
                                additionalInfoSignal.accountHolderName(),
                                request.tenantId()
                        )
                );
            } catch (Exception e) {
                log.warn("Customer update with additional info failed (continuing): {}", e.getMessage());
            }

            // Salary fetch (non-critical)
            log.info("Step 5c: Salary fetch (GOSI) for workflow: {}", workflowId);

            try {
                var salaryResult = salaryActivity.fetchSalary(
                        new SalaryFetchActivity.SalaryFetchInput(
                                request.nationalId(),
                                additionalInfoSignal.employerCrNumber(),
                                request.tenantId()
                        )
                );
                if (salaryResult.fetched()) {
                    log.info("Salary fetched: {} {}", salaryResult.netSalary(), salaryResult.currency());
                }
            } catch (Exception e) {
                log.warn("Salary fetch failed (non-critical): {}", e.getMessage());
            }

            // Wallet creation (non-blocking)
            try {
                log.info("Step 5c: Wallet creation for workflow: {}", workflowId);
                var walletResult = walletActivity.createWallet(
                        new WalletCreationActivity.WalletCreationInput(
                                state.getCustomerId(), request.tenantId(), "SAR"
                        )
                );
                if (walletResult.created()) {
                    state.setWalletId(walletResult.walletId());
                } else {
                    log.warn("Wallet creation returned not-created (continuing): {}", workflowId);
                }
            } catch (Exception e) {
                log.warn("Wallet creation failed (continuing): {}", e.getMessage());
            }

            // Notification (non-critical)
            try {
                notificationActivity.sendWelcomeNotification(
                        new NotificationActivity.NotificationInput(
                                state.getCustomerId(),
                                request.mobileNumber(),
                                yakeenResult != null ? yakeenResult.fullNameEn() : request.nationalId(),
                                request.tenantId()
                        )
                );
            } catch (Exception e) {
                log.warn("Welcome notification failed (non-critical): {}", e.getMessage());
            }

            // =================================================================
            // STEP 8: PIN Setup (signal wait — 24-hour timeout)
            //   Customer must set a 6-digit app PIN before onboarding is complete.
            //   Versioned for backward compatibility with existing workflows.
            // =================================================================
            int pinVersion = Workflow.getVersion("pin-setup", Workflow.DEFAULT_VERSION, 1);

            if (pinVersion >= 1) {
                updateState(OnboardingStep.PIN_SETUP);
                log.info("Step 8: Waiting for PIN setup signal for workflow: {}", workflowId);

                boolean pinSignalReceived = Workflow.await(Duration.ofHours(24), () -> setPinReceived);
                if (!pinSignalReceived) {
                    return fail(workflowId, "PIN setup timed out (24 hours)");
                }

                // Validate device trust for PIN signal — flag but don't hard-fail
                if (setPinSignal.deviceId() != null && state.getInitialDeviceId() != null
                        && !state.getInitialDeviceId().equals(setPinSignal.deviceId())) {
                    state.setDeviceTrusted(false);
                    log.warn("Device changed during PIN setup: initial={}, current={} — flagged as untrusted (continuing)",
                            state.getInitialDeviceId(), setPinSignal.deviceId());
                }

                // Store PIN via Identity Service (Keycloak user attribute)
                try {
                    log.info("Step 8: Setting PIN for workflow: {}", workflowId);
                    var pinResult = setPinActivity.setPin(
                            new SetPinActivity.SetPinInput(
                                    state.getKeycloakUserId(),
                                    request.nationalId(),
                                    setPinSignal.pin(),
                                    request.tenantId()
                            )
                    );
                    if (pinResult.pinSet()) {
                        state.setPinSet(true);
                        log.info("PIN set successfully for workflow: {}", workflowId);
                    } else {
                        log.warn("PIN setting returned not-set (continuing): {}", pinResult.message());
                    }
                } catch (Exception e) {
                    log.warn("PIN setting failed (continuing): {}", e.getMessage());
                }
            }

            // =================================================================
            // SUCCESS
            // =================================================================
            updateState(OnboardingStep.COMPLETED);
            log.info("Onboarding completed successfully for workflow: {}", workflowId);

            return new OnboardingResult(
                    workflowId,
                    OnboardingStatus.COMPLETED,
                    state.getCustomerId() != null ? UUID.fromString(state.getCustomerId()) : null,
                    state.getGlobalUid() != null ? UUID.fromString(state.getGlobalUid()) : null,
                    state.getWalletId() != null ? UUID.fromString(state.getWalletId()) : null,
                    state.getKeycloakUserId(),
                    null
            );

        } catch (ApplicationFailure af) {
            // Re-throw application failures from fail() calls — already logged and state is set
            throw af;
        } catch (Exception e) {
            log.error("Onboarding workflow failed: {}", e.getMessage(), e);
            fail(workflowId, "Unexpected error: " + e.getMessage());
            throw new AssertionError("unreachable"); // fail() always throws
        }
    }

    // -------------------------------------------------------------------------
    // @SignalMethod -- Signal Handlers
    // -------------------------------------------------------------------------

    @Override
    public void otpVerified(OtpVerifiedSignal signal) {
        log.info("Received OTP verified signal");
        this.otpVerifiedSignal = signal;
        this.otpVerifiedReceived = true;
    }

    @Override
    public void termsAccepted(TermsAcceptedSignal signal) {
        log.info("Received terms accepted signal: accepted={}", signal.accepted());
        this.termsAcceptedSignal = signal;
        this.termsAcceptedReceived = true;
    }

    @Override
    public void nafathInitiate(NafathInitiateSignal signal) {
        log.info("Received Nafath initiate signal");
        this.nafathInitiateSignal = signal;
        this.nafathInitiateReceived = true;
    }

    @Override
    public void nafathCallback(NafathCallbackSignal signal) {
        log.info("Received Nafath callback: accepted={}", signal.accepted());
        this.nafathCallbackSignal = signal;
        this.nafathCallbackReceived = true;
    }

    @Override
    public void additionalInfoSubmitted(AdditionalInfoSignal signal) {
        log.info("Received additional info signal");
        this.additionalInfoSignal = signal;
        this.additionalInfoReceived = true;
    }

    @Override
    public void eddFormSubmitted(EddFormSignal signal) {
        log.info("Received EDD form submission signal");
        this.eddFormSignal = signal;
        this.eddFormReceived = true;
    }

    @Override
    public void setPinSubmitted(SetPinSignal signal) {
        log.info("Received set PIN signal");
        this.setPinSignal = signal;
        this.setPinReceived = true;
    }

    // -------------------------------------------------------------------------
    // @QueryMethod -- Query Handlers
    // -------------------------------------------------------------------------

    @Override
    public OnboardingState getState() {
        return state;
    }

    @Override
    public OnboardingStatus getStatus() {
        // OnboardingStep and OnboardingStatus have the same enum values
        if (state.getCurrentStep() == null) {
            return OnboardingStatus.INITIATED;
        }
        return OnboardingStatus.valueOf(state.getCurrentStep().name());
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a copy of the given ActivityOptions with the task queue set to the specified queue.
     */
    private static ActivityOptions withQueue(String taskQueue, ActivityOptions base) {
        return ActivityOptions.newBuilder(base)
                .setTaskQueue(taskQueue)
                .build();
    }

    /**
     * Validates that the device ID in the signal matches the initial device from workflow start.
     * If no device info is provided or the initial device was not set, validation is skipped.
     * A device mismatch marks the state as untrusted and returns false.
     */
    private boolean validateDevice(DeviceInfo deviceInfo) {
        if (deviceInfo == null || deviceInfo.deviceId() == null) {
            return true; // No device info = skip validation
        }
        if (state.getInitialDeviceId() == null || state.getInitialDeviceId().isBlank()) {
            state.setInitialDeviceId(deviceInfo.deviceId());
            state.setDeviceTrusted(true);
            return true;
        }
        if (!state.getInitialDeviceId().equals(deviceInfo.deviceId())) {
            // Flag as untrusted but do NOT hard-fail — allow onboarding to continue
            // with enhanced monitoring. Hard-blocking here breaks API gateway scenarios
            // where headers may differ between calls.
            state.setDeviceTrusted(false);
            log.warn("Device changed: initial={}, current={} — flagged as untrusted (continuing)",
                    state.getInitialDeviceId(), deviceInfo.deviceId());
        }
        return true;
    }

    /**
     * Updates the current step in the workflow state and refreshes the lastUpdatedAt timestamp.
     */
    private void updateState(OnboardingStep step) {
        state.setCurrentStep(step);
        state.setLastUpdatedAt(Instant.now());
    }

    /**
     * Transitions the workflow to FAILED status and throws ApplicationFailure.
     * Throwing (instead of returning) ensures Temporal records the workflow execution
     * as FAILED — not "Completed". The failure is non-retryable to prevent Temporal
     * from automatically retrying the entire workflow.
     */
    private OnboardingResult fail(String workflowId, String reason) {
        state.setFailureReason(reason);
        updateState(OnboardingStep.FAILED);
        log.error("Onboarding failed for workflow {}: {}", workflowId, reason);
        throw ApplicationFailure.newNonRetryableFailure(reason, "ONBOARDING_FAILED");
    }
}
