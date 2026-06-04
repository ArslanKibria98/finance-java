package com.ksa.financing.onboarding.foreign.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignBiometricsRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignConfirmDataRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignInitiateRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignInitiateResponse;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignPassportDataResponse;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignSetPinRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignStatusResponse;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignStepInfo;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignStepResponse;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignUploadPassportRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignUploadSelfieRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignUploadSelfieResponse;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignVerifyOtpRequest;
import com.ksa.financing.onboarding.foreign.application.dto.ForeignVerifyOtpResponse;
import com.ksa.financing.onboarding.foreign.application.usecase.ForeignWorkflowClient;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignDataConfirmedSignal;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingRequest;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingState;
import com.ksa.financing.onboarding.foreign.domain.model.ForeignOnboardingStep;
import com.ksa.financing.onboarding.foreign.domain.port.in.ConfirmForeignDataUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.EnableForeignBiometricsUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.GetForeignStatusUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.SetForeignPinUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.StartForeignOnboardingUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.UploadForeignPassportUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.UploadForeignSelfieUseCase;
import com.ksa.financing.onboarding.foreign.domain.port.in.VerifyForeignDualOtpUseCase;
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
 * REST endpoints for the country-agnostic foreign national onboarding flow.
 *
 * <p>Passport-only KYC: no document-type selection step. Captures country of origin and
 * country of residence at {@code /initiate}. Mirrors the Canada full flow but without
 * the ID-vs-Passport branch.</p>
 */
@RestController
@RequestMapping("/api/v1/onboarding/foreign")
public class ForeignOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(ForeignOnboardingController.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    private final StartForeignOnboardingUseCase startUseCase;
    private final VerifyForeignDualOtpUseCase verifyOtpUseCase;
    private final UploadForeignPassportUseCase uploadPassportUseCase;
    private final ConfirmForeignDataUseCase confirmDataUseCase;
    private final UploadForeignSelfieUseCase uploadSelfieUseCase;
    private final SetForeignPinUseCase setPinUseCase;
    private final EnableForeignBiometricsUseCase biometricsUseCase;
    private final GetForeignStatusUseCase statusUseCase;
    private final ForeignWorkflowClient workflowClient;
    private final PreFlowRiskGate preFlowRiskGate;

    public ForeignOnboardingController(StartForeignOnboardingUseCase startUseCase,
                                       VerifyForeignDualOtpUseCase verifyOtpUseCase,
                                       UploadForeignPassportUseCase uploadPassportUseCase,
                                       ConfirmForeignDataUseCase confirmDataUseCase,
                                       UploadForeignSelfieUseCase uploadSelfieUseCase,
                                       SetForeignPinUseCase setPinUseCase,
                                       EnableForeignBiometricsUseCase biometricsUseCase,
                                       GetForeignStatusUseCase statusUseCase,
                                       ForeignWorkflowClient workflowClient,
                                       PreFlowRiskGate preFlowRiskGate) {
        this.startUseCase = startUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.uploadPassportUseCase = uploadPassportUseCase;
        this.confirmDataUseCase = confirmDataUseCase;
        this.uploadSelfieUseCase = uploadSelfieUseCase;
        this.setPinUseCase = setPinUseCase;
        this.biometricsUseCase = biometricsUseCase;
        this.statusUseCase = statusUseCase;
        this.workflowClient = workflowClient;
        this.preFlowRiskGate = preFlowRiskGate;
    }

    // ==================== Step 1: Initiate ====================

    @PostMapping("/initiate")
    @Operation(summary = "Initiate foreign national onboarding",
            description = "Start onboarding — captures country of origin + residence, sends OTP to mobile and email (stub: 123456).",
            tags = "1. Foreign — Initiate")
    public ResponseEntity<ForeignInitiateResponse> initiate(
            @Valid @RequestBody ForeignInitiateRequest request,
            HttpServletRequest httpRequest) {
        log.info("Foreign initiate for email={} origin={} residence={}",
                mask(request.email()), request.countryOfOrigin(), request.residentialCountry());
        DeviceInfo deviceInfo = device(httpRequest);
        String tenantId = tenant(httpRequest);

        // Pre-flow risk + fraud gate — velocity, blacklist, route checks against mobile.
        // Fail-closed: any non-PASS decision blocks the workflow from starting.
        var gate = preFlowRiskGate.check(null, request.mobileNumber(), tenantId, deviceInfo, httpRequest);
        if (!gate.allowed()) {
            log.warn("Foreign onboarding BLOCKED by pre-flow risk gate: status={}, assessmentId={}",
                    gate.status(), gate.assessmentId());
            HttpStatus blockStatus = "ROUTE_LOGIN".equals(gate.status())
                    || "ROUTE_REONBOARDING".equals(gate.status())
                    ? HttpStatus.OK : HttpStatus.FORBIDDEN;
            return ResponseEntity.status(blockStatus).body(new ForeignInitiateResponse(
                    null,
                    "ROUTE_LOGIN".equals(gate.status()) || "ROUTE_REONBOARDING".equals(gate.status())
                            ? "COMPLETE" : "BLOCKED",
                    "BLOCKED",
                    gate.routeTo() != null ? gate.routeTo() : "NONE",
                    null, null, null, null,
                    gate.message(),
                    Instant.now().toString()));
        }

        var domainRequest = new ForeignOnboardingRequest(
                request.email(), request.mobileNumber(),
                request.countryOfOrigin(), request.residentialCountry(),
                tenantId, deviceInfo);
        var result = startUseCase.start(domainRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ForeignInitiateResponse(
                result.workflowId(),
                result.status(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
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
            description = "Verifies both OTPs in one call. Creates Keycloak user, returns JWT.",
            tags = "2. Foreign — OTP")
    public ResponseEntity<ForeignVerifyOtpResponse> verifyOtp(
            @Valid @RequestBody ForeignVerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = verifyOtpUseCase.verify(workflowId, request.mobileOtp(),
                request.emailOtp(), device(httpRequest));
        // OTP retries keep workflow at OTP_SENT; only failureReason==null means
        // both OTPs verified and workflow advanced to OTP_VERIFIED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ForeignVerifyOtpResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
                result.keycloakUserId(),
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                result.tokenType(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 3: Upload passport → Facia OCR ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping(value = "/upload-passport", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload passport image (multipart file) to Sullis for OCR + authenticity",
            tags = "3. Foreign — Passport",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ForeignPassportDataResponse> uploadPassport(
            @RequestParam("email") String email,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(email);
        var result = uploadPassportUseCase.submit(workflowId, toBase64(file), device(httpRequest));
        // Document-verification retries keep workflow at OTP_VERIFIED; only failureReason==null
        // means Facia accepted the passport and workflow advanced to PASSPORT_UPLOADED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ForeignPassportDataResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
                result.faciaReferenceId(),
                result.extractedData(),
                result.message(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(email)));
    }

    // ==================== Step 4: Confirm extracted data ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/confirm-data")
    @Operation(summary = "Confirm or correct passport OCR fields",
            tags = "4. Foreign — Data",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ForeignStepResponse> confirmData(
            @Valid @RequestBody ForeignConfirmDataRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var signal = new ForeignDataConfirmedSignal(
                request.surname(), request.givenName(), request.nationality(),
                request.dateOfBirth(), request.passportNumber(),
                request.issueDate(), request.expiryDate(),
                request.homeAddress(), request.countryOfOrigin(), request.residentialCountry(),
                device(httpRequest));
        var result = confirmDataUseCase.confirm(workflowId, signal);
        return ResponseEntity.ok(new ForeignStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
                false,
                result.message(),
                null,
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 5: Upload selfie → face match → customer + wallet ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping(value = "/upload-selfie", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload selfie (multipart file) for Sullis face-match + verification",
            tags = "5. Foreign — Selfie",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ForeignUploadSelfieResponse> uploadSelfie(
            @RequestParam("email") String email,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(email);
        var result = uploadSelfieUseCase.submit(workflowId, toBase64(file), device(httpRequest));
        // Face match retries keep workflow at DATA_CONFIRMED; only failureReason==null
        // means the selfie was actually accepted and workflow advanced to SELFIE_VERIFIED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ForeignUploadSelfieResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
                result.faciaReferenceId(),
                result.faceMatchScore(),
                result.customerId(),
                result.walletId(),
                result.message(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(email)));
    }

    // ==================== Step 6: Set PIN ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/set-pin")
    @Operation(summary = "Set 6-digit app PIN",
            tags = "6. Foreign — PIN",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ForeignStepResponse> setPin(
            @Valid @RequestBody ForeignSetPinRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = setPinUseCase.setPin(workflowId, request.pin(),
                request.confirmPin(), device(httpRequest));
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new ForeignStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
                false,
                result.message(),
                result.failureReason(),
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 7: Enable biometrics → COMPLETED ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/enable-biometrics")
    @Operation(summary = "Enable Face ID / Touch ID (can be skipped) — completes onboarding",
            tags = "7. Foreign — Biometrics",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ForeignStepResponse> enableBiometrics(
            @Valid @RequestBody ForeignBiometricsRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = biometricsUseCase.submit(workflowId, request.enabled(),
                request.skipped(), device(httpRequest));
        return ResponseEntity.ok(new ForeignStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                ForeignStepInfo.nextAction(result.currentStep()),
                result.onboardingComplete(),
                result.message(),
                null,
                Instant.now().toString(),
                resolveMobile(request.email())));
    }

    // ==================== Step 8: Status ====================

    @GetMapping("/status")
    @Operation(summary = "Get current foreign onboarding state by email",
            tags = "8. Foreign — Status")
    public ResponseEntity<ForeignStatusResponse> status(@RequestParam String email) {
        ForeignOnboardingState s = statusUseCase.getStatusByEmail(email);
        return ResponseEntity.ok(new ForeignStatusResponse(
                s.getWorkflowId(),
                s.getCurrentStep() != null ? s.getCurrentStep().name() : null,
                s.getCurrentStep() != null ? s.getCurrentStep().name() : null,
                ForeignStepInfo.nextAction(s.getCurrentStep()),
                s.getEmail(),
                s.getMobileNumber(),
                s.getCountryOfOrigin(),
                s.getResidentialCountry(),
                s.getCustomerId(),
                s.getWalletId(),
                s.getKeycloakUserId(),
                s.getGlobalUid(),
                s.getFaceMatchScore(),
                s.isPinSet(),
                s.isBiometricsEnabled(),
                s.isOnboardingComplete(),
                s.getExtractedData(),
                s.getConfirmedData(),
                s.getFailureReason(),
                s.getStartedAt() != null ? s.getStartedAt().toString() : null,
                s.getLastUpdatedAt() != null ? s.getLastUpdatedAt().toString() : null));
    }

    // ====================  Helpers ====================

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
            ForeignOnboardingState state = statusUseCase.getStatusByEmail(email);
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
