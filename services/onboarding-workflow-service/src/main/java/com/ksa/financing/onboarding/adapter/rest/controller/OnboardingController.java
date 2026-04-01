package com.ksa.financing.onboarding.adapter.rest.controller;

import com.ksa.financing.onboarding.application.dto.*;
import com.ksa.financing.onboarding.domain.model.*;
import com.ksa.financing.onboarding.domain.port.in.*;
import com.ksa.financing.onboarding.workflow.CustomerOnboardingWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.temporal.client.WorkflowClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the signal-driven customer onboarding flow.
 *
 * <p>Endpoints are split into public (Steps 1-2, Nafath callback) and private/JWT (Steps 3-5, status).
 * The controller orchestrates REST calls to external services (KYC Adapter for OTP, IDS for Keycloak
 * user creation) and signals the Temporal workflow for state transitions.</p>
 *
 * <p>Device metadata (deviceId, location, IP, user-agent) is extracted from HTTP headers:
 * X-Device-Id, X-Latitude, X-Longitude, X-Client-Ip, X-User-Agent.</p>
 */
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private static final Logger log = LoggerFactory.getLogger(OnboardingController.class);

    private final StartOnboardingUseCase startOnboardingUseCase;
    private final GetOnboardingStatusUseCase getOnboardingStatusUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final InitiateNafathUseCase initiateNafathUseCase;
    private final SubmitAdditionalInfoUseCase submitAdditionalInfoUseCase;
    private final WorkflowClient workflowClient;
    private final RestTemplate restTemplate;

    @Value("${app.services.kyc-adapter-url}")
    private String kycAdapterUrl;

    @Value("${app.services.identity-url}")
    private String identityUrl;

    @Value("${app.services.risk-service-url}")
    private String riskServiceUrl;

    @Value("${app.services.customer-service-url}")
    private String customerServiceUrl;

    public OnboardingController(StartOnboardingUseCase startOnboardingUseCase,
                                GetOnboardingStatusUseCase getOnboardingStatusUseCase,
                                VerifyOtpUseCase verifyOtpUseCase,
                                AcceptTermsUseCase acceptTermsUseCase,
                                InitiateNafathUseCase initiateNafathUseCase,
                                SubmitAdditionalInfoUseCase submitAdditionalInfoUseCase,
                                WorkflowClient workflowClient,
                                RestTemplate restTemplate) {
        this.startOnboardingUseCase = startOnboardingUseCase;
        this.getOnboardingStatusUseCase = getOnboardingStatusUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.acceptTermsUseCase = acceptTermsUseCase;
        this.initiateNafathUseCase = initiateNafathUseCase;
        this.submitAdditionalInfoUseCase = submitAdditionalInfoUseCase;
        this.workflowClient = workflowClient;
        this.restTemplate = restTemplate;
    }

    // ==================== Step 1: Initiate (PUBLIC) ====================

    @PostMapping("/initiate")
    @Operation(summary = "Initiate onboarding", description = "Start or resume onboarding — runs Tahakuk + sends OTP", tags = "1. Initiate Onboarding")
    public ResponseEntity<InitiateOnboardingResponse> initiate(
            @Valid @RequestBody StartOnboardingRequest request,
            HttpServletRequest httpRequest) {

        // Validate NID format (10 digits, starts with 1=citizen or 2=resident)
        NationalId.of(request.nationalId());

        log.info("Onboarding initiate for NID ending ...{}", maskNid(request.nationalId()));
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);
        String tenantId = extractTenantIdFromHeader(httpRequest);

        // ===== RISK GATE: Call internal checks API before starting workflow =====
        var riskGateResult = runInternalChecksGate(
                request.nationalId(), request.mobileNumber(), tenantId,
                deviceInfo, httpRequest);

        if (riskGateResult != null) {
            // Internal checks failed — return error, do NOT start workflow
            return riskGateResult;
        }
        // ===== END RISK GATE =====

        OnboardingRequest domainRequest = new OnboardingRequest(
                request.nationalId(),
                request.mobileNumber(),
                tenantId,
                deviceInfo.deviceId(),
                deviceInfo.latitude(),
                deviceInfo.longitude(),
                null
        );

        StartOnboardingUseCase.StartOnboardingResult result = startOnboardingUseCase.start(domainRequest);

        OnboardingStep currentStep = result.currentStep();
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? OnboardingStep.INITIATED : null;
        List<StepInfo> steps = StepInfo.buildSteps(currentStep, failedAtStep);
        String nextAction = StepInfo.getNextAction(currentStep);

        InitiateOnboardingResponse response = new InitiateOnboardingResponse(
                result.workflowId(),
                result.status(),
                currentStep != null ? currentStep.name() : "INITIATED",
                nextAction,
                result.otpRequestId(),
                result.maskedMobile(),
                result.globalUid(),
                result.customerId(),
                result.failureReason(),
                steps,
                Instant.now().toString()
        );

        log.info("Onboarding initiated: workflowId={}, status={}", result.workflowId(), result.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== Step 2: OTP Verify (PUBLIC) ====================

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify OTP code, create Keycloak user, return JWT tokens", tags = "2. OTP Verification")
    @SuppressWarnings("unchecked")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        String workflowId = "onboarding-" + request.nationalId();
        log.info("OTP verification for workflow: {}", workflowId);
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);

        // Get otpRequestId from workflow state
        OnboardingState state = getOnboardingStatusUseCase.getStatus(workflowId);
        String nationalId = request.nationalId();
        String otpRequestId = state.getOtpRequestId();

        // Guard: reject if workflow is already FAILED or not at OTP_SENT step
        if (state.getCurrentStep() == OnboardingStep.FAILED) {
            log.warn("OTP verify rejected: workflow {} is FAILED. Reason: {}", workflowId, state.getFailureReason());
            return ResponseEntity.badRequest().body(new VerifyOtpResponse(
                    workflowId, "FAILED",
                    OnboardingStep.FAILED.name(),
                    StepInfo.getNextAction(OnboardingStep.FAILED),
                    state.getGlobalUid(), state.getCustomerId(),
                    null, null, 0, null,
                    "Onboarding has failed. Please start a new registration.",
                    StepInfo.buildSteps(OnboardingStep.FAILED, OnboardingStep.OTP_SENT),
                    Instant.now().toString()
            ));
        }

        if (state.getCurrentStep() != OnboardingStep.OTP_SENT) {
            log.warn("OTP verify rejected: workflow {} is in step {}, expected OTP_SENT",
                    workflowId, state.getCurrentStep());
            return ResponseEntity.badRequest().body(new VerifyOtpResponse(
                    workflowId, state.getCurrentStep() != null ? state.getCurrentStep().name() : "UNKNOWN",
                    state.getCurrentStep() != null ? state.getCurrentStep().name() : "UNKNOWN",
                    StepInfo.getNextAction(state.getCurrentStep()),
                    state.getGlobalUid(), state.getCustomerId(),
                    null, null, 0, null,
                    "OTP verification is not expected at this stage.",
                    StepInfo.buildSteps(state.getCurrentStep()),
                    Instant.now().toString()
            ));
        }

        // Step 2a: Verify OTP via KYC Adapter
        Map<String, String> otpVerifyPayload = new LinkedHashMap<>();
        otpVerifyPayload.put("nationalId", nationalId);
        otpVerifyPayload.put("otpCode", request.otpCode());
        otpVerifyPayload.put("otpRequestId", otpRequestId);

        ResponseEntity<Map> otpResponse = restTemplate.postForEntity(
                kycAdapterUrl + "/api/v1/kyc/otp/verify",
                otpVerifyPayload,
                Map.class
        );

        Map<String, Object> otpBody = unwrapApiResponse(otpResponse.getBody());
        if (otpBody == null || !Boolean.TRUE.equals(otpBody.get("verified"))) {
            String reason = otpBody != null ? (String) otpBody.get("failureReason") : "OTP verification failed";
            log.warn("OTP verification failed for workflow: {} reason: {}", workflowId, reason);

            OnboardingStep failStep = OnboardingStep.OTP_SENT;
            return ResponseEntity.badRequest().body(new VerifyOtpResponse(
                    workflowId, "OTP_FAILED",
                    failStep.name(),
                    StepInfo.getNextAction(failStep),
                    state.getGlobalUid(), state.getCustomerId(),
                    null, null, 0, null,
                    reason,
                    StepInfo.buildSteps(failStep),
                    Instant.now().toString()
            ));
        }

        // Step 2b: Create Keycloak user via IDS
        String keycloakUserId = null;
        String accessToken = null;
        String refreshToken = null;
        long expiresIn = 0;

        try {
            Map<String, String> registerPayload = new LinkedHashMap<>();
            registerPayload.put("nationalId", nationalId);
            registerPayload.put("mobileNumber", state.getMobileNumber());
            registerPayload.put("globalUid", state.getGlobalUid());

            ResponseEntity<Map> idsResponse = restTemplate.postForEntity(
                    identityUrl + "/api/v1/auth/onboarding-register",
                    registerPayload,
                    Map.class
            );

            Map<String, Object> idsBody = unwrapApiResponse(idsResponse.getBody());
            if (idsBody != null) {
                keycloakUserId = (String) idsBody.get("keycloakUserId");
                accessToken = (String) idsBody.get("accessToken");
                refreshToken = (String) idsBody.get("refreshToken");
                expiresIn = idsBody.get("expiresIn") != null ? ((Number) idsBody.get("expiresIn")).longValue() : 0;
            } else {
                log.warn("IDS registration returned null for workflow: {} (continuing without Keycloak user)", workflowId);
            }
        } catch (Exception e) {
            log.warn("IDS registration failed for workflow: {} (continuing without Keycloak user): {}",
                    workflowId, e.getMessage());
        }

        // Step 2c: Signal the workflow
        verifyOtpUseCase.verify(
                workflowId, nationalId, request.otpCode(), otpRequestId,
                keycloakUserId, deviceInfo
        );

        // Query updated state after signal
        OnboardingState updatedState = queryStateWithRetry(workflowId, OnboardingStep.OTP_VERIFIED);
        OnboardingStep currentStep = updatedState.getCurrentStep() != null
                ? updatedState.getCurrentStep() : OnboardingStep.OTP_VERIFIED;
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

        log.info("OTP verified and Keycloak user created for workflow: {}", workflowId);
        return ResponseEntity.ok(new VerifyOtpResponse(
                workflowId,
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                updatedState.getGlobalUid(),
                updatedState.getCustomerId(),
                accessToken, refreshToken, expiresIn, "Bearer",
                null,
                StepInfo.buildSteps(currentStep, failedAtStep),
                Instant.now().toString()
        ));
    }

    // ==================== Step 2: OTP Resend (PUBLIC) ====================

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Resend OTP code via KYC Adapter", tags = "2. OTP Verification")
    @SuppressWarnings("unchecked")
    public ResponseEntity<OnboardingStepResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        String workflowId = "onboarding-" + request.nationalId();
        log.info("OTP resend for workflow: {}", workflowId);

        OnboardingState state = getOnboardingStatusUseCase.getStatus(workflowId);

        Map<String, String> resendPayload = new LinkedHashMap<>();
        resendPayload.put("nationalId", request.nationalId());
        resendPayload.put("mobileNumber", request.mobileNumber());
        resendPayload.put("otpRequestId", state.getOtpRequestId());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                kycAdapterUrl + "/api/v1/kyc/otp/resend",
                resendPayload,
                Map.class
        );

        Map<String, Object> body = unwrapApiResponse(response.getBody());
        boolean sent = body != null && Boolean.TRUE.equals(body.get("sent"));
        String newOtpRequestId = body != null ? (String) body.get("otpRequestId") : null;

        OnboardingStep currentStep = state.getCurrentStep() != null
                ? state.getCurrentStep() : OnboardingStep.OTP_SENT;

        String message = sent
                ? "OTP resent successfully. New OTP request ID: " + newOtpRequestId
                : "Failed to resend OTP";

        return ResponseEntity.ok(new OnboardingStepResponse(
                workflowId,
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                state.getGlobalUid(),
                state.getCustomerId(),
                message,
                sent ? null : "OTP resend failed",
                null,
                StepInfo.buildSteps(currentStep),
                Instant.now().toString()
        ));
    }

    // ==================== Step 3: Accept Terms (JWT) ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/accept-terms")
    @Operation(summary = "Accept terms", description = "Accept or decline terms and conditions", tags = "3. Accept Terms")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OnboardingStepResponse> acceptTerms(
            @Valid @RequestBody AcceptTermsRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        extractTenantId(jwt); // validate tenant present in JWT
        String workflowId = "onboarding-" + request.nationalId();
        log.info("Terms acceptance for workflow: {} accepted: {}", workflowId, request.accepted());
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);

        AcceptTermsUseCase.AcceptTermsResult result = acceptTermsUseCase.accept(
                workflowId, request.accepted(), deviceInfo
        );

        // Query updated state after signal
        OnboardingState updatedState = queryStateWithRetry(workflowId, OnboardingStep.TERMS_ACCEPTED);
        OnboardingStep currentStep = updatedState.getCurrentStep() != null
                ? updatedState.getCurrentStep() : OnboardingStep.TERMS_ACCEPTED;
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

        return ResponseEntity.ok(new OnboardingStepResponse(
                workflowId,
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                updatedState.getGlobalUid(),
                updatedState.getCustomerId(),
                result.message(),
                updatedState.getFailureReason(),
                null,
                StepInfo.buildSteps(currentStep, failedAtStep),
                Instant.now().toString()
        ));
    }

    // ==================== Step 4: Nafath Initiate (JWT) ====================

    @PostMapping("/initiate-nafath")
    @Operation(summary = "Initiate Nafath", description = "Start Nafath verification — returns random number and transactionId", tags = "4. Nafath Verification")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NafathInitiateResponse> initiateNafath(
            @Valid @RequestBody InitiateNafathRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        extractTenantId(jwt); // validate tenant present in JWT
        String workflowId = "onboarding-" + request.nationalId();
        log.info("Nafath initiation for workflow: {}", workflowId);
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);

        InitiateNafathUseCase.InitiateNafathResult result = initiateNafathUseCase.initiate(
                workflowId, deviceInfo
        );

        // Query updated state after signal + activity execution
        OnboardingState updatedState = queryStateWithRetry(workflowId, OnboardingStep.NAFATH_INITIATED);
        OnboardingStep currentStep = updatedState.getCurrentStep() != null
                ? updatedState.getCurrentStep() : OnboardingStep.NAFATH_INITIATED;
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

        return ResponseEntity.ok(new NafathInitiateResponse(
                result.workflowId(),
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                updatedState.getGlobalUid(),
                updatedState.getCustomerId(),
                result.nafathRandomNumber(),
                result.nafathSessionId(),
                result.transactionId(),
                StepInfo.buildSteps(currentStep, failedAtStep),
                Instant.now().toString()
        ));
    }

    // ==================== Step 4b: Nafath Callback (PUBLIC — Webhook) ====================

    @PostMapping("/nafath-callback")
    @Operation(summary = "Nafath callback", description = "Webhook for Nafath verification result", tags = "4. Nafath Verification")
    public ResponseEntity<OnboardingStepResponse> nafathCallback(
            @Valid @RequestBody NafathCallbackSignal signal) {

        String workflowId = "onboarding-" + signal.nationalId();
        log.info("Nafath callback for workflow: {} accepted: {}", workflowId, signal.accepted());

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);
            workflow.nafathCallback(signal);

            // Query updated state — wait for auto steps (Yakeen + Sanctions + Profile) to finish
            OnboardingState updatedState = queryStateWithRetry(workflowId, OnboardingStep.INFO_PENDING);
            OnboardingStep currentStep = updatedState.getCurrentStep() != null
                    ? updatedState.getCurrentStep() : OnboardingStep.INFO_PENDING;
            OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

            String message = signal.accepted()
                    ? "Nafath verification successful"
                    : "Nafath verification rejected: " + signal.rejectionReason();

            return ResponseEntity.ok(new OnboardingStepResponse(
                    workflowId,
                    currentStep.name(),
                    currentStep.name(),
                    StepInfo.getNextAction(currentStep),
                    updatedState.getGlobalUid(),
                    updatedState.getCustomerId(),
                    message,
                    updatedState.getFailureReason(),
                    updatedState.getNafathVerificationData(),
                    StepInfo.buildSteps(currentStep, failedAtStep),
                    Instant.now().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to process Nafath callback for workflow: {}", workflowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OnboardingStepResponse(
                    workflowId, "ERROR", "NAFATH_INITIATED",
                    "RETRY_NAFATH_CALLBACK",
                    null, null,
                    null,
                    "Failed to process Nafath callback: " + e.getMessage(),
                    null,
                    StepInfo.buildSteps(OnboardingStep.NAFATH_INITIATED),
                    Instant.now().toString()
            ));
        }
    }

    // ==================== Step 5: Submit Additional Info (JWT) ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/submit-info")
    @Operation(summary = "Submit additional info", description = "Submit employment, banking details and isPep flag. If isPep=true, triggers EDD flow", tags = "5. Additional Info")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OnboardingStepResponse> submitAdditionalInfo(
            @Valid @RequestBody SubmitAdditionalInfoRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        extractTenantId(jwt); // validate tenant present in JWT
        String workflowId = "onboarding-" + request.nationalId();
        log.info("Additional info submission for workflow: {}", workflowId);
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);

        // Verify workflow is in INFO_PENDING state (ready for additional info)
        OnboardingState currentState = getOnboardingStatusUseCase.getStatus(workflowId);
        if (currentState.getCurrentStep() != OnboardingStep.INFO_PENDING) {
            log.warn("Submit-info rejected: workflow {} is in step {}, expected INFO_PENDING",
                    workflowId, currentState.getCurrentStep());
            return ResponseEntity.badRequest().body(new OnboardingStepResponse(
                    workflowId,
                    currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN",
                    currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN",
                    StepInfo.getNextAction(currentState.getCurrentStep()),
                    currentState.getGlobalUid(),
                    currentState.getCustomerId(),
                    null,
                    "Additional info can only be submitted when workflow is in INFO_PENDING state. Current step: "
                            + (currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN"),
                    null,
                    StepInfo.buildSteps(currentState.getCurrentStep()),
                    Instant.now().toString()
            ));
        }

        AdditionalInfoSignal signal = new AdditionalInfoSignal(
                request.email(),
                request.employerName(),
                request.employerCrNumber(),
                request.employmentType(),
                request.jobTitle(),
                request.basicSalary(),
                request.grossSalary(),
                request.netSalary(),
                request.currency(),
                request.bankName(),
                request.bankCode(),
                request.iban(),
                request.accountHolderName(),
                deviceInfo,
                Boolean.TRUE.equals(request.isPep()),
                request.sourceOfFunds(),
                request.estimatedNetWorth(),
                request.sourceOfIncome()
        );

        SubmitAdditionalInfoUseCase.SubmitAdditionalInfoResult result =
                submitAdditionalInfoUseCase.submit(workflowId, signal);

        // Wait for workflow to reach PIN_SETUP (both isPep=true and false go to PIN_SETUP now)
        OnboardingStep expectedStep = OnboardingStep.PIN_SETUP;
        OnboardingState updatedState = queryStateWithRetry(workflowId, expectedStep, 20, 500L);
        OnboardingStep currentStep = updatedState.getCurrentStep() != null
                ? updatedState.getCurrentStep() : OnboardingStep.SCREENING;
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

        return ResponseEntity.ok(new OnboardingStepResponse(
                workflowId,
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                updatedState.getGlobalUid(),
                updatedState.getCustomerId(),
                result.message(),
                updatedState.getFailureReason(),
                null,
                StepInfo.buildSteps(currentStep, failedAtStep),
                Instant.now().toString()
        ));
    }

    // ==================== Step 6: Submit EDD Form (JWT) ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/submit-edd")
    @Operation(summary = "Submit EDD form", description = "Submit Enhanced Due Diligence form when isPep=true (EDD_REQUIRED state)", tags = "6. EDD Form")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OnboardingStepResponse> submitEddForm(
            @Valid @RequestBody SubmitEddRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        extractTenantId(jwt); // validate tenant present in JWT
        String workflowId = "onboarding-" + request.nationalId();
        log.info("EDD form submission for workflow: {}", workflowId);
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);

        // Verify workflow is in EDD_REQUIRED state
        OnboardingState currentState = getOnboardingStatusUseCase.getStatus(workflowId);
        if (currentState.getCurrentStep() != OnboardingStep.EDD_REQUIRED) {
            log.warn("EDD submission rejected: workflow {} is in step {}, expected EDD_REQUIRED",
                    workflowId, currentState.getCurrentStep());
            return ResponseEntity.badRequest().body(new OnboardingStepResponse(
                    workflowId,
                    currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN",
                    currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN",
                    StepInfo.getNextAction(currentState.getCurrentStep()),
                    currentState.getGlobalUid(),
                    currentState.getCustomerId(),
                    null,
                    "EDD form can only be submitted when workflow is in EDD_REQUIRED state",
                    null,
                    StepInfo.buildSteps(currentState.getCurrentStep()),
                    Instant.now().toString()
            ));
        }

        // Convert related persons from DTO to signal model
        var relatedPersons = request.relatedPersons() != null
                ? request.relatedPersons().stream()
                    .map(rp -> new EddFormSignal.RelatedPerson(rp.name(), rp.relationship(), rp.position()))
                    .toList()
                : java.util.List.<EddFormSignal.RelatedPerson>of();

        // Send EDD signal to workflow
        EddFormSignal signal = new EddFormSignal(
                deviceInfo.deviceId(),
                request.politicalPosition(),
                request.governmentBody(),
                request.countryOfInfluence(),
                request.positionStartDate(),
                request.positionEndDate(),
                request.primarySourceOfWealth(),
                request.estimatedNetWorth(),
                request.sourceOfWealthDescription(),
                request.sourceOfFunds(),
                request.sourceOfFundsDetails(),
                relatedPersons,
                request.additionalNotes()
        );

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);
            workflow.eddFormSubmitted(signal);
        } catch (Exception e) {
            log.error("Failed to send EDD signal for workflow: {}", workflowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OnboardingStepResponse(
                    workflowId, "ERROR", "EDD_REQUIRED",
                    "RETRY_EDD_SUBMISSION",
                    currentState.getGlobalUid(), currentState.getCustomerId(),
                    null,
                    "Failed to submit EDD form: " + e.getMessage(),
                    null,
                    StepInfo.buildSteps(OnboardingStep.EDD_REQUIRED),
                    Instant.now().toString()
            ));
        }

        // Query updated state — wait for risk decision + wallet + PIN_SETUP
        OnboardingState updatedState = queryStateWithRetry(workflowId, OnboardingStep.PIN_SETUP, 20, 500);
        OnboardingStep currentStep = updatedState.getCurrentStep() != null
                ? updatedState.getCurrentStep() : OnboardingStep.EDD_SUBMITTED;
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

        return ResponseEntity.ok(new OnboardingStepResponse(
                workflowId,
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                updatedState.getGlobalUid(),
                updatedState.getCustomerId(),
                "EDD form submitted successfully",
                updatedState.getFailureReason(),
                null,
                StepInfo.buildSteps(currentStep, failedAtStep),
                Instant.now().toString()
        ));
    }

    // ==================== Step 7: Set PIN (JWT) ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/set-pin")
    @Operation(summary = "Set app PIN", description = "Set a 6-digit app PIN after wallet creation and before onboarding completion", tags = "7. Set PIN")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<OnboardingStepResponse> setPin(
            @Valid @RequestBody SetPinRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        extractTenantId(jwt); // validate tenant present in JWT
        String workflowId = "onboarding-" + request.nationalId();
        log.info("PIN setup for workflow: {}", workflowId);
        DeviceInfo deviceInfo = extractDeviceInfo(httpRequest);

        // Verify workflow is in PIN_SETUP state
        OnboardingState currentState = getOnboardingStatusUseCase.getStatus(workflowId);
        if (currentState.getCurrentStep() != OnboardingStep.PIN_SETUP) {
            log.warn("PIN setup rejected: workflow {} is in step {}, expected PIN_SETUP",
                    workflowId, currentState.getCurrentStep());
            return ResponseEntity.badRequest().body(new OnboardingStepResponse(
                    workflowId,
                    currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN",
                    currentState.getCurrentStep() != null ? currentState.getCurrentStep().name() : "UNKNOWN",
                    StepInfo.getNextAction(currentState.getCurrentStep()),
                    currentState.getGlobalUid(),
                    currentState.getCustomerId(),
                    null,
                    "PIN can only be set when workflow is in PIN_SETUP state",
                    null,
                    StepInfo.buildSteps(currentState.getCurrentStep()),
                    Instant.now().toString()
            ));
        }

        // Validate PIN and confirmPin match
        if (!request.pin().equals(request.confirmPin())) {
            return ResponseEntity.badRequest().body(new OnboardingStepResponse(
                    workflowId,
                    "PIN_SETUP",
                    "PIN_SETUP",
                    "SET_PIN",
                    currentState.getGlobalUid(),
                    currentState.getCustomerId(),
                    null,
                    "PIN and Confirm PIN do not match",
                    null,
                    StepInfo.buildSteps(OnboardingStep.PIN_SETUP),
                    Instant.now().toString()
            ));
        }

        // Validate PIN security rules
        String pinError = validatePinSecurity(request.pin(), request.nationalId(), currentState);
        if (pinError != null) {
            return ResponseEntity.badRequest().body(new OnboardingStepResponse(
                    workflowId,
                    "PIN_SETUP",
                    "PIN_SETUP",
                    "SET_PIN",
                    currentState.getGlobalUid(),
                    currentState.getCustomerId(),
                    null,
                    pinError,
                    null,
                    StepInfo.buildSteps(OnboardingStep.PIN_SETUP),
                    Instant.now().toString()
            ));
        }

        // Send set-pin signal to workflow
        SetPinSignal signal = new SetPinSignal(
                deviceInfo.deviceId(),
                request.pin(),
                request.confirmPin()
        );

        try {
            CustomerOnboardingWorkflow workflow = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);
            workflow.setPinSubmitted(signal);
        } catch (Exception e) {
            log.error("Failed to send set-pin signal for workflow: {}", workflowId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new OnboardingStepResponse(
                    workflowId, "ERROR", "PIN_SETUP",
                    "RETRY_SET_PIN",
                    currentState.getGlobalUid(), currentState.getCustomerId(),
                    null,
                    "Failed to set PIN: " + e.getMessage(),
                    null,
                    StepInfo.buildSteps(OnboardingStep.PIN_SETUP),
                    Instant.now().toString()
            ));
        }

        // Query updated state — wait for PIN to be set and workflow to complete
        OnboardingState updatedState = queryStateWithRetry(workflowId, OnboardingStep.COMPLETED);
        OnboardingStep currentStep = updatedState.getCurrentStep() != null
                ? updatedState.getCurrentStep() : OnboardingStep.COMPLETED;
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED) ? inferFailedStep(updatedState) : null;

        return ResponseEntity.ok(new OnboardingStepResponse(
                workflowId,
                currentStep.name(),
                currentStep.name(),
                StepInfo.getNextAction(currentStep),
                updatedState.getGlobalUid(),
                updatedState.getCustomerId(),
                "PIN set successfully — onboarding complete",
                updatedState.getFailureReason(),
                null,
                StepInfo.buildSteps(currentStep, failedAtStep),
                Instant.now().toString()
        ));
    }

    // ==================== Status Query (PUBLIC) ====================

    @GetMapping("/status")
    @Operation(summary = "Get onboarding status", description = "Query full onboarding state including Yakeen data", tags = "8. Status")
    public ResponseEntity<OnboardingStatusResponse> getStatus(
            @RequestParam @NotBlank String nationalId) {
        String workflowId = "onboarding-" + nationalId;
        log.debug("Status query for workflow: {}", workflowId);

        OnboardingState state = getOnboardingStatusUseCase.getStatus(workflowId);
        OnboardingStep currentStep = state.getCurrentStep();

        // If no workflow exists, check if customer is already registered
        if (currentStep == OnboardingStep.INITIATED && state.getCustomerId() == null) {
            if (isCustomerRegistered(nationalId)) {
                var completedSteps = StepInfo.buildSteps(OnboardingStep.COMPLETED);
                OnboardingStatusResponse completeResponse = new OnboardingStatusResponse(
                        workflowId, "COMPLETED", "COMPLETED", "LOGIN",
                        null, null, null, null, null, 0, false, null,
                        null,
                        null, null, completedSteps,
                        null, null
                );
                return ResponseEntity.ok(completeResponse);
            }
        }

        // If failureReason exists and workflow didn't complete successfully, force FAILED
        if (state.getFailureReason() != null && !state.getFailureReason().isBlank()
                && currentStep != OnboardingStep.FAILED
                && currentStep != OnboardingStep.COMPLETED) {
            currentStep = OnboardingStep.FAILED;
        }
        // If workflow completed successfully, clear any leftover failure reason
        if (currentStep == OnboardingStep.COMPLETED && state.getFailureReason() != null) {
            state.setFailureReason(null);
        }

        // Infer which step failed from state data
        OnboardingStep failedAtStep = (currentStep == OnboardingStep.FAILED)
                ? inferFailedStep(state) : null;

        OnboardingStatusResponse response = new OnboardingStatusResponse(
                state.getWorkflowId(),
                currentStep != null ? currentStep.name() : null,
                currentStep != null ? currentStep.name() : null,
                StepInfo.getNextAction(currentStep),
                state.getCustomerId(),
                state.getGlobalUid(),
                state.getWalletId(),
                state.getKeycloakUserId(),
                state.getOtpRequestId(),
                state.getNafathRandomNumber(),
                state.isDeviceTrusted(),
                state.getLifecycleStage(),
                state.getFailureReason(),
                state.getNafathVerificationData(),
                state.getYakeenData(),
                StepInfo.buildSteps(currentStep, failedAtStep),
                state.getStartedAt() != null ? state.getStartedAt().toString() : null,
                state.getLastUpdatedAt() != null ? state.getLastUpdatedAt().toString() : null
        );

        return ResponseEntity.ok(response);
    }

    // ==================== Active Onboardings (super_admin) ====================

    @SecuredEndpoint(obj = "onboarding", act = "read")
    @GetMapping("/active")
    @Operation(summary = "List all active onboarding workflows",
            description = "Returns all running onboarding workflows from Temporal. super_admin only.",
            tags = "8. Status",
            security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<List<Map<String, Object>>> listActiveOnboardings(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Listing active onboardings requested by: {}", jwt.getSubject());

        List<OnboardingState> activeWorkflows = getOnboardingStatusUseCase.listActiveOnboardings();

        List<Map<String, Object>> results = activeWorkflows.stream().map(state -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("workflowId", state.getWorkflowId());
            entry.put("nationalId", state.getNationalId());
            entry.put("mobileNumber", state.getMobileNumber());
            entry.put("currentStep", state.getCurrentStep() != null ? state.getCurrentStep().name() : null);
            entry.put("lifecycleStage", state.getLifecycleStage());
            entry.put("customerId", state.getCustomerId());
            entry.put("globalUid", state.getGlobalUid());
            entry.put("deviceTrusted", state.isDeviceTrusted());
            entry.put("startedAt", state.getStartedAt() != null ? state.getStartedAt().toString() : null);
            entry.put("lastUpdatedAt", state.getLastUpdatedAt() != null ? state.getLastUpdatedAt().toString() : null);
            return entry;
        }).toList();

        return ResponseEntity.ok(results);
    }

    // ==================== Private Helpers ====================

    private static final String DEFAULT_TENANT_ID = "54b53072-540e-3eb8-b8e9-343e71f28176";

    /**
     * Extracts tenant_id from X-Tenant-Id header (for public/pre-auth endpoints).
     * Falls back to default tenant if header is missing.
     */
    private String extractTenantIdFromHeader(HttpServletRequest httpRequest) {
        String tenantId = httpRequest.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isBlank()) {
            log.debug("No X-Tenant-Id header found, using default tenant");
            return DEFAULT_TENANT_ID;
        }
        return tenantId;
    }

    /**
     * Extracts tenant_id from JWT claims (for authenticated endpoints).
     */
    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private DeviceInfo extractDeviceInfo(HttpServletRequest httpRequest) {
        return new DeviceInfo(
                httpRequest.getHeader("X-Device-Id"),
                httpRequest.getHeader("X-Latitude"),
                httpRequest.getHeader("X-Longitude"),
                httpRequest.getHeader("X-Client-Ip"),
                httpRequest.getHeader("X-User-Agent")
        );
    }

    private OnboardingStep inferFailedStep(OnboardingState state) {
        // Check from latest step backwards. Note: customerId and globalUid are
        // pre-generated at workflow start, so they can't be used for inference.
        if (state.getWalletId() != null) return OnboardingStep.COMPLETED;
        if (state.getLifecycleStage() != null && state.getLifecycleStage().equals("QUALIFIED"))
            return OnboardingStep.INFO_PENDING;
        if (state.getYakeenData() != null) return OnboardingStep.INFO_PENDING;
        if (state.getNafathSessionId() != null) return OnboardingStep.NAFATH_VERIFIED;
        if (state.getKeycloakUserId() != null) return OnboardingStep.TERMS_ACCEPTED;
        if (state.getOtpRequestId() != null) return OnboardingStep.OTP_VERIFIED;
        return OnboardingStep.INITIATED;
    }

    private String maskNid(String nid) {
        if (nid == null || nid.length() < 4) {
            return "****";
        }
        return "****" + nid.substring(nid.length() - 4);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapApiResponse(Map<String, Object> body) {
        if (body != null && body.containsKey("data") && body.get("data") instanceof Map) {
            return (Map<String, Object>) body.get("data");
        }
        return body;
    }

    /**
     * Validates PIN against security rules:
     * - No sequential numbers (3+ ascending or descending)
     * - No repeating numbers (3+ consecutive same digits)
     * - No DOB usage (DDMMYY, YYMMDD, MMDDYY)
     * - No NID subset (any 6 consecutive digits of NID)
     * - Not in common PIN blacklist
     */
    private String validatePinSecurity(String pin, String nationalId, OnboardingState state) {
        // No sequential numbers (ascending or descending, 3+ digits)
        for (int i = 0; i <= pin.length() - 3; i++) {
            int d1 = pin.charAt(i) - '0';
            int d2 = pin.charAt(i + 1) - '0';
            int d3 = pin.charAt(i + 2) - '0';
            if (d2 - d1 == 1 && d3 - d2 == 1) {
                return "PIN must not contain ascending sequential numbers";
            }
            if (d1 - d2 == 1 && d2 - d3 == 1) {
                return "PIN must not contain descending sequential numbers";
            }
        }

        // No repeating numbers (3+ consecutive same digits)
        for (int i = 0; i <= pin.length() - 3; i++) {
            if (pin.charAt(i) == pin.charAt(i + 1) && pin.charAt(i + 1) == pin.charAt(i + 2)) {
                return "PIN must not contain 3 or more consecutive repeated digits";
            }
        }

        // No DOB usage — check various date formats
        if (state.getYakeenData() != null) {
            Object dobObj = state.getYakeenData().get("dateOfBirth");
            if (dobObj != null) {
                String dob = dobObj.toString().replace("-", "").replace("/", "");
                // Try DDMMYY, YYMMDD, MMDDYY patterns (6-digit subsets of DOB)
                if (dob.length() >= 8) {
                    String ddmmyy = dob.substring(6, 8) + dob.substring(4, 6) + dob.substring(2, 4);
                    String yymmdd = dob.substring(2, 4) + dob.substring(4, 6) + dob.substring(6, 8);
                    String mmddyy = dob.substring(4, 6) + dob.substring(6, 8) + dob.substring(2, 4);
                    if (pin.equals(ddmmyy) || pin.equals(yymmdd) || pin.equals(mmddyy)) {
                        return "PIN must not match your date of birth";
                    }
                }
            }
        }

        // No NID subset (any 6 consecutive digits of NID)
        if (nationalId != null && nationalId.length() >= 6) {
            for (int i = 0; i <= nationalId.length() - 6; i++) {
                if (pin.equals(nationalId.substring(i, i + 6))) {
                    return "PIN must not be a subset of your National ID";
                }
            }
        }

        // Common PIN blacklist
        List<String> blacklist = List.of(
                "000000", "111111", "222222", "333333", "444444",
                "555555", "666666", "777777", "888888", "999999",
                "123123", "696969", "112233", "121212"
        );
        if (blacklist.contains(pin)) {
            return "PIN is too common and not allowed";
        }

        return null; // Valid
    }

    private OnboardingState queryStateWithRetry(String workflowId, OnboardingStep expectedStep) {
        return queryStateWithRetry(workflowId, expectedStep, 10, 300);
    }

    private OnboardingState queryStateWithRetry(String workflowId, OnboardingStep expectedStep,
                                                int maxRetries, long sleepMillis) {
        try {
            CustomerOnboardingWorkflow stub = workflowClient.newWorkflowStub(
                    CustomerOnboardingWorkflow.class, workflowId);

            for (int i = 0; i < maxRetries; i++) {
                OnboardingState state = stub.getState();
                if (state.getCurrentStep() != null
                        && state.getCurrentStep().ordinal() >= expectedStep.ordinal()) {
                    return state;
                }
                if (state.getCurrentStep() == OnboardingStep.FAILED) {
                    return state;
                }
                Thread.sleep(sleepMillis);
            }
            return stub.getState();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new OnboardingState();
        } catch (Exception e) {
            log.warn("Could not query workflow state for {}: {}", workflowId, e.getMessage());
            return new OnboardingState();
        }
    }

    // ==================== RISK GATE: Internal Checks ====================

    /**
     * Calls risk-service POST /api/v1/risk/internal-checks BEFORE starting the workflow.
     * If the result is not PASS, returns a response that blocks onboarding.
     * If the result is PASS, returns null (caller should proceed).
     *
     * <p>Fail-closed: if risk-service is unreachable, onboarding is blocked.</p>
     */
    @SuppressWarnings("unchecked")
    private ResponseEntity<InitiateOnboardingResponse> runInternalChecksGate(
            String nationalId, String mobileNumber, String tenantId,
            DeviceInfo deviceInfo, HttpServletRequest httpRequest) {

        String url = riskServiceUrl + "/api/v1/risk/internal-checks";

        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Tenant-Id", tenantId);
            if (deviceInfo.deviceId() != null) headers.set("X-Device-Id", deviceInfo.deviceId());
            if (httpRequest.getHeader("X-Device-Fingerprint") != null) {
                headers.set("X-Device-Fingerprint", httpRequest.getHeader("X-Device-Fingerprint"));
            }
            if (deviceInfo.ipAddress() != null) headers.set("X-Client-Ip", deviceInfo.ipAddress());
            if (httpRequest.getHeader("X-Session-Id") != null) {
                headers.set("X-Session-Id", httpRequest.getHeader("X-Session-Id"));
            }

            var body = Map.of(
                    "nationalId", nationalId,
                    "mobileNumber", mobileNumber
            );

            var request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getBody() == null) {
                log.warn("Risk-service returned null — failing closed");
                return buildRiskGateBlockResponse("HARD_BLOCK",
                        "Service temporarily unavailable. Please try again later.", null);
            }

            // Unwrap {data: {...}}
            Map<String, Object> data = response.getBody();
            if (data.containsKey("data") && data.get("data") instanceof Map) {
                data = (Map<String, Object>) data.get("data");
            }

            String decision = data.get("overallDecision") != null ? data.get("overallDecision").toString() : null;
            String blockReason = (String) data.get("blockReason");
            String routeTo = (String) data.get("routeTo");
            String assessmentId = (String) data.get("assessmentId");

            log.info("Risk gate result: assessmentId={}, decision={}", assessmentId, decision);

            if ("PASS".equals(decision)) {
                // All checks passed — proceed with onboarding
                return null;
            }

            // Not PASS — block the onboarding
            if ("HARD_BLOCK".equals(decision)) {
                log.warn("Onboarding BLOCKED by internal checks: assessmentId={}", assessmentId);
                return buildRiskGateBlockResponse("BLOCKED",
                        blockReason != null ? blockReason : "Unable to proceed with registration at this time.",
                        null);
            }

            if ("ROUTE_LOGIN".equals(decision)) {
                log.info("Onboarding redirected to LOGIN: assessmentId={}", assessmentId);
                return buildRiskGateBlockResponse("ROUTE_LOGIN",
                        blockReason != null ? blockReason
                                : "An account already exists with this ID. Please log in.",
                        "LOGIN");
            }

            if ("ROUTE_REONBOARDING".equals(decision)) {
                log.info("Onboarding redirected to WELCOME_BACK: assessmentId={}", assessmentId);
                return buildRiskGateBlockResponse("ROUTE_REONBOARDING",
                        "Welcome back! Your account is inactive. Please reactivate.",
                        "WELCOME_BACK");
            }

            // Any other non-PASS decision — block
            log.warn("Onboarding blocked by unknown decision: {}", decision);
            return buildRiskGateBlockResponse(decision,
                    blockReason != null ? blockReason : "Unable to proceed with registration at this time.",
                    routeTo);

        } catch (Exception e) {
            // Fail-closed: risk-service down = onboarding blocked
            log.error("Risk-service internal checks call failed — failing closed: {}", e.getMessage());
            return buildRiskGateBlockResponse("HARD_BLOCK",
                    "Service temporarily unavailable. Please try again later.", null);
        }
    }

    private boolean isCustomerRegistered(String nationalId) {
        try {
            String url = customerServiceUrl + "/internal/customers/exists/" + nationalId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                // Unwrap {data: {exists: true}} envelope
                if (body.containsKey("data") && body.get("data") instanceof Map) {
                    body = (Map<String, Object>) body.get("data");
                }
                return Boolean.TRUE.equals(body.get("exists"));
            }
        } catch (Exception e) {
            log.warn("Customer existence check failed for NID ending ...{}: {}",
                    nationalId.substring(nationalId.length() - 4), e.getMessage());
        }
        return false;
    }

    private ResponseEntity<InitiateOnboardingResponse> buildRiskGateBlockResponse(
            String status, String message, String routeTo) {
        String effectiveStatus = ("ROUTE_LOGIN".equals(status) || "ROUTE_REONBOARDING".equals(status))
                ? "COMPLETE" : status;
        var response = new InitiateOnboardingResponse(
                null,           // no workflowId — workflow was never started
                effectiveStatus,
                "BLOCKED",
                routeTo != null ? routeTo : "NONE",
                null, null, null, null,
                message,
                List.of(),
                Instant.now().toString()
        );
        if ("ROUTE_LOGIN".equals(status) || "ROUTE_REONBOARDING".equals(status)) {
            return ResponseEntity.ok(response);
        }
        // HARD_BLOCK and others → 403 Forbidden
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
