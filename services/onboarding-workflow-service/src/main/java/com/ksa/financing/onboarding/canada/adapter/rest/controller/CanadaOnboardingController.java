package com.ksa.financing.onboarding.canada.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.onboarding.canada.application.dto.CanadaBiometricsRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaConfirmDataRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaDocumentDataResponse;
import com.ksa.financing.onboarding.canada.application.dto.CanadaInitiateRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaInitiateResponse;
import com.ksa.financing.onboarding.canada.application.dto.CanadaSelectDocumentRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaSetPinRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaStatusResponse;
import com.ksa.financing.onboarding.canada.application.dto.CanadaStepInfo;
import com.ksa.financing.onboarding.canada.application.dto.CanadaStepResponse;
import com.ksa.financing.onboarding.canada.application.dto.CanadaSubmitDocumentRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaSubmitSelfieRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaSubmitSelfieResponse;
import com.ksa.financing.onboarding.canada.application.dto.CanadaVerifyOtpRequest;
import com.ksa.financing.onboarding.canada.application.dto.CanadaVerifyOtpResponse;
import com.ksa.financing.onboarding.canada.application.usecase.CanadaWorkflowClient;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingRequest;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingState;
import com.ksa.financing.onboarding.canada.domain.model.CanadaOnboardingStep;
import com.ksa.financing.onboarding.canada.domain.model.DocConfirmedSignal;
import com.ksa.financing.onboarding.canada.domain.port.in.ConfirmDocumentDataUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.EnableBiometricsUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.GetCanadaOnboardingStatusUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.SelectDocumentUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.SetCanadaPinUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.StartCanadaOnboardingUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.SubmitDocumentUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.SubmitSelfieUseCase;
import com.ksa.financing.onboarding.canada.domain.port.in.VerifyDualOtpUseCase;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.shared.risk.PreFlowRiskGate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * REST endpoints for the Canada onboarding flow.
 *
 * <p>Mirrors the KSA controller pattern: each step calls a use case which signals the
 * Temporal workflow, then queries the resulting state to build a response.</p>
 *
 * <p>All endpoints are public so they can be called pre-token. Phase 2 will move
 * Step 3 onwards behind JWT once the issued token is wired into the gateway.</p>
 */
@RestController
@RequestMapping("/api/v1/onboarding/ca")
public class CanadaOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(CanadaOnboardingController.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    private final StartCanadaOnboardingUseCase startUseCase;
    private final VerifyDualOtpUseCase verifyOtpUseCase;
    private final SelectDocumentUseCase selectDocumentUseCase;
    private final SubmitDocumentUseCase submitDocumentUseCase;
    private final ConfirmDocumentDataUseCase confirmDataUseCase;
    private final SubmitSelfieUseCase submitSelfieUseCase;
    private final SetCanadaPinUseCase setPinUseCase;
    private final EnableBiometricsUseCase biometricsUseCase;
    private final GetCanadaOnboardingStatusUseCase statusUseCase;
    private final CanadaWorkflowClient workflowClient;
    private final PreFlowRiskGate preFlowRiskGate;

    public CanadaOnboardingController(StartCanadaOnboardingUseCase startUseCase,
                                      VerifyDualOtpUseCase verifyOtpUseCase,
                                      SelectDocumentUseCase selectDocumentUseCase,
                                      SubmitDocumentUseCase submitDocumentUseCase,
                                      ConfirmDocumentDataUseCase confirmDataUseCase,
                                      SubmitSelfieUseCase submitSelfieUseCase,
                                      SetCanadaPinUseCase setPinUseCase,
                                      EnableBiometricsUseCase biometricsUseCase,
                                      GetCanadaOnboardingStatusUseCase statusUseCase,
                                      CanadaWorkflowClient workflowClient,
                                      PreFlowRiskGate preFlowRiskGate) {
        this.startUseCase = startUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.selectDocumentUseCase = selectDocumentUseCase;
        this.submitDocumentUseCase = submitDocumentUseCase;
        this.confirmDataUseCase = confirmDataUseCase;
        this.submitSelfieUseCase = submitSelfieUseCase;
        this.setPinUseCase = setPinUseCase;
        this.biometricsUseCase = biometricsUseCase;
        this.statusUseCase = statusUseCase;
        this.workflowClient = workflowClient;
        this.preFlowRiskGate = preFlowRiskGate;
    }

    // ==================== Step 1: Initiate ====================

    @PostMapping("/initiate")
    @Operation(summary = "Initiate Canada onboarding",
            description = "Start onboarding — sends OTP to mobile and email (stub: code 123456)",
            tags = "1. Canada — Initiate")
    public ResponseEntity<CanadaInitiateResponse> initiate(
            @Valid @RequestBody CanadaInitiateRequest request,
            HttpServletRequest httpRequest) {
        log.info("Canada initiate for email={}", mask(request.email()));
        DeviceInfo deviceInfo = device(httpRequest);
        String tenantId = tenant(httpRequest);

        // Pre-flow risk + fraud gate — velocity, blacklist, route checks against mobile.
        // Fail-closed: any non-PASS decision blocks the workflow from starting.
        var gate = preFlowRiskGate.check(null, request.mobileNumber(), tenantId, deviceInfo, httpRequest);
        if (!gate.allowed()) {
            log.warn("Canada onboarding BLOCKED by pre-flow risk gate: status={}, assessmentId={}",
                    gate.status(), gate.assessmentId());
            HttpStatus blockStatus = "ROUTE_LOGIN".equals(gate.status())
                    || "ROUTE_REONBOARDING".equals(gate.status())
                    ? HttpStatus.OK : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(blockStatus).body(new CanadaInitiateResponse(
                    null,
                    "ROUTE_LOGIN".equals(gate.status()) || "ROUTE_REONBOARDING".equals(gate.status())
                            ? "COMPLETE" : "BLOCKED",
                    "BLOCKED",
                    gate.routeTo() != null ? gate.routeTo() : "NONE",
                    null, null, null, null,
                    gate.message(),
                    Instant.now().toString()));
        }

        var domainRequest = new CanadaOnboardingRequest(
                request.email(), request.mobileNumber(), tenantId, deviceInfo);
        var result = startUseCase.start(domainRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CanadaInitiateResponse(
                result.workflowId(),
                result.status(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.mobileOtpRequestId(),
                result.emailOtpRequestId(),
                result.maskedMobile(),
                result.maskedEmail(),
                result.failureReason(),
                Instant.now().toString()));
    }

    // ==================== Step 2: Verify dual OTP ====================

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify mobile + email OTP",
            description = "Verifies both OTPs in one call. Returns JWT-like token on success.",
            tags = "2. Canada — OTP")
    public ResponseEntity<CanadaVerifyOtpResponse> verifyOtp(
            @Valid @RequestBody CanadaVerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = verifyOtpUseCase.verify(workflowId, request.mobileOtp(),
                request.emailOtp(), device(httpRequest));
        // OTP retries keep workflow at OTP_SENT; only failureReason==null means both
        // OTPs verified and workflow advanced to OTP_VERIFIED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new CanadaVerifyOtpResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.keycloakUserId(),
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                result.tokenType(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 3: Select document type ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/select-document")
    @Operation(summary = "Select document type (ID or PASSPORT)",
            tags = "3. Canada — Document",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CanadaStepResponse> selectDocument(
            @Valid @RequestBody CanadaSelectDocumentRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = selectDocumentUseCase.select(workflowId, request.documentType(), device(httpRequest));
        return ResponseEntity.ok(new CanadaStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.message(),
                null,
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 4: Upload document → Facia OCR ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping(value = "/upload-document", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document image (multipart file) to Sullis for OCR + authenticity",
            tags = "4. Canada — Document",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CanadaDocumentDataResponse> uploadDocument(
            @RequestParam("email") String email,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(email);
        var result = submitDocumentUseCase.submit(workflowId, toBase64(file), device(httpRequest));
        // Document-verification retries keep workflow at DOC_SELECTED; only failureReason==null
        // means Facia accepted and workflow advanced to DOC_VERIFIED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new CanadaDocumentDataResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.faciaReferenceId(),
                result.extractedData(),
                result.message(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(email)));
    }

    // ==================== Step 5: Confirm extracted data ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/confirm-data")
    @Operation(summary = "Confirm or correct OCR-extracted fields",
            tags = "5. Canada — Document",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CanadaStepResponse> confirmData(
            @Valid @RequestBody CanadaConfirmDataRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var signal = new DocConfirmedSignal(
                request.surname(), request.givenName(), request.nationality(),
                request.dateOfBirth(), request.documentNumber(), request.homeAddress(),
                device(httpRequest));
        var result = confirmDataUseCase.confirm(workflowId, signal);
        return ResponseEntity.ok(new CanadaStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.message(),
                null,
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 6: Upload selfie → Facia face-match ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping(value = "/upload-selfie", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload selfie (multipart file) for Sullis face-match + verification",
            tags = "6. Canada — Selfie",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CanadaSubmitSelfieResponse> uploadSelfie(
            @RequestParam("email") String email,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(email);
        var result = submitSelfieUseCase.submit(workflowId, toBase64(file), device(httpRequest));
        // Face-match retries keep workflow at DOC_CONFIRMED; only failureReason==null
        // means Facia matched and workflow advanced to SELFIE_VERIFIED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new CanadaSubmitSelfieResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.faciaReferenceId(),
                result.faceMatchScore(),
                result.customerId(),
                result.walletId(),
                result.message(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(email)));
    }

    // ==================== Step 7: Set PIN ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/set-pin")
    @Operation(summary = "Set 6-digit app PIN",
            tags = "7. Canada — PIN",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CanadaStepResponse> setPin(
            @Valid @RequestBody CanadaSetPinRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = setPinUseCase.setPin(workflowId, request.pin(),
                request.confirmPin(), device(httpRequest));
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new CanadaStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.message(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 8: Enable biometrics (or skip) ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/enable-biometrics")
    @Operation(summary = "Enable Face ID / Touch ID (can be skipped) — completes onboarding",
            tags = "8. Canada — Biometrics",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CanadaStepResponse> enableBiometrics(
            @Valid @RequestBody CanadaBiometricsRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = biometricsUseCase.submit(workflowId, request.enabled(),
                request.skipped(), device(httpRequest));
        return ResponseEntity.ok(new CanadaStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                CanadaStepInfo.nextAction(result.currentStep()),
                result.message(),
                null,
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 9: Status ====================

    @GetMapping("/status")
    @Operation(summary = "Get current Canada onboarding state by email",
            tags = "9. Canada — Status")
    public ResponseEntity<CanadaStatusResponse> status(@RequestParam String email) {
        CanadaOnboardingState state = statusUseCase.getStatusByEmail(email);
        return ResponseEntity.ok(new CanadaStatusResponse(
                state.getWorkflowId(),
                state.getCurrentStep() != null ? state.getCurrentStep().name() : null,
                state.getCurrentStep() != null ? state.getCurrentStep().name() : null,
                CanadaStepInfo.nextAction(state.getCurrentStep()),
                state.getEmail(),
                state.getMobileNumber(),
                state.getDocumentType(),
                state.getCustomerId(),
                state.getWalletId(),
                state.getKeycloakUserId(),
                state.getGlobalUid(),
                state.getFaceMatchScore(),
                state.isPinSet(),
                state.isBiometricsEnabled(),
                state.getExtractedData(),
                state.getConfirmedData(),
                state.getFailureReason(),
                state.getStartedAt() != null ? state.getStartedAt().toString() : null,
                state.getLastUpdatedAt() != null ? state.getLastUpdatedAt().toString() : null));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private DeviceInfo device(HttpServletRequest req) {
        return new DeviceInfo(
                req.getHeader("X-Device-Id"),
                req.getHeader("X-Latitude"),
                req.getHeader("X-Longitude"),
                req.getHeader("X-Client-Ip"),
                req.getHeader("X-User-Agent"));
    }

    /**
     * Read the uploaded image part and Base64-encode it. The internal flow (Temporal
     * signals, workflow, Sullis activity, document archive) keeps using base64 strings,
     * so only the REST contract changes from JSON-base64 to a multipart file.
     */
    private static String toBase64(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.ksa.financing.infra.exception.BusinessException(
                    com.ksa.financing.infra.exception.ErrorCodes.BAD_REQUEST,
                    "Multipart 'file' part is required");
        }
        try {
            return java.util.Base64.getEncoder().encodeToString(file.getBytes());
        } catch (java.io.IOException e) {
            throw new com.ksa.financing.infra.exception.BusinessException(
                    com.ksa.financing.infra.exception.ErrorCodes.BAD_REQUEST,
                    "Failed to read uploaded file: " + e.getMessage());
        }
    }

    /**
     * Best-effort raw mobile lookup so every onboarding step's audit log carries
     * the phone number for Kibana journey tracing (not just the initiate step).
     * Never fails the request — returns null on any error.
     */
    private String resolveMobile(String email) {
        try {
            CanadaOnboardingState state = statusUseCase.getStatusByEmail(email);
            return state != null ? state.getMobileNumber() : null;
        } catch (RuntimeException e) {
            log.debug("resolveMobile failed for audit enrichment (email={}): {}",
                    mask(email), e.getMessage());
            return null;
        }
    }

    private String tenant(HttpServletRequest req) {
        String t = req.getHeader("X-Tenant-Id");
        if (t == null || t.isBlank()) return DEFAULT_TENANT_ID;
        try {
            UUID.fromString(t);
            return t;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid X-Tenant-Id header, using default");
            return DEFAULT_TENANT_ID;
        }
    }

    private static String mask(String email) {
        if (email == null || !email.contains("@")) return "****";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
