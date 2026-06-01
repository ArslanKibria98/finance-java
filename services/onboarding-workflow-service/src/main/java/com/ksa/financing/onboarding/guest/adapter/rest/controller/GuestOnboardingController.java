package com.ksa.financing.onboarding.guest.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.onboarding.domain.model.DeviceInfo;
import com.ksa.financing.onboarding.guest.application.dto.GuestBiometricsRequest;
import com.ksa.financing.onboarding.guest.application.dto.GuestInitiateRequest;
import com.ksa.financing.onboarding.guest.application.dto.GuestInitiateResponse;
import com.ksa.financing.onboarding.guest.application.dto.GuestSetPinRequest;
import com.ksa.financing.onboarding.guest.application.dto.GuestStatusResponse;
import com.ksa.financing.onboarding.guest.application.dto.GuestStepInfo;
import com.ksa.financing.onboarding.guest.application.dto.GuestStepResponse;
import com.ksa.financing.onboarding.guest.application.dto.GuestVerifyOtpRequest;
import com.ksa.financing.onboarding.guest.application.dto.GuestVerifyOtpResponse;
import com.ksa.financing.onboarding.guest.application.usecase.GuestWorkflowClient;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingRequest;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingState;
import com.ksa.financing.onboarding.guest.domain.model.GuestOnboardingStep;
import com.ksa.financing.onboarding.guest.domain.port.in.EnableGuestBiometricsUseCase;
import com.ksa.financing.onboarding.guest.domain.port.in.GetGuestStatusUseCase;
import com.ksa.financing.onboarding.guest.domain.port.in.SetGuestPinUseCase;
import com.ksa.financing.onboarding.guest.domain.port.in.StartGuestOnboardingUseCase;
import com.ksa.financing.onboarding.guest.domain.port.in.VerifyGuestDualOtpUseCase;
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
 * REST endpoints for the country-agnostic guest onboarding flow.
 *
 * <p>Short 4-step flow that lets a user reach the dashboard without doing full KYC.
 * The state carries {@code onboardingComplete=false} so callers know the user still
 * needs to finish the document + selfie KYC flow before regulated features unlock.</p>
 */
@RestController
@RequestMapping("/api/v1/onboarding/guest")
public class GuestOnboardingController {

    private static final Logger log = LoggerFactory.getLogger(GuestOnboardingController.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    private final StartGuestOnboardingUseCase startUseCase;
    private final VerifyGuestDualOtpUseCase verifyOtpUseCase;
    private final EnableGuestBiometricsUseCase biometricsUseCase;
    private final SetGuestPinUseCase setPinUseCase;
    private final GetGuestStatusUseCase statusUseCase;
    private final GuestWorkflowClient workflowClient;

    public GuestOnboardingController(StartGuestOnboardingUseCase startUseCase,
                                     VerifyGuestDualOtpUseCase verifyOtpUseCase,
                                     EnableGuestBiometricsUseCase biometricsUseCase,
                                     SetGuestPinUseCase setPinUseCase,
                                     GetGuestStatusUseCase statusUseCase,
                                     GuestWorkflowClient workflowClient) {
        this.startUseCase = startUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.biometricsUseCase = biometricsUseCase;
        this.setPinUseCase = setPinUseCase;
        this.statusUseCase = statusUseCase;
        this.workflowClient = workflowClient;
    }

    // ==================== Step 1: Initiate ====================

    @PostMapping("/initiate")
    @Operation(summary = "Initiate guest onboarding",
            description = "Start guest onboarding — sends OTP to mobile and email (stub: 123456). " +
                    "Resulting workflow always carries onboardingComplete=false.",
            tags = "1. Guest — Initiate")
    public ResponseEntity<GuestInitiateResponse> initiate(
            @Valid @RequestBody GuestInitiateRequest request,
            HttpServletRequest httpRequest) {
        log.info("Guest initiate for email={}", mask(request.email()));
        DeviceInfo deviceInfo = device(httpRequest);
        String tenantId = tenant(httpRequest);

        var domainRequest = new GuestOnboardingRequest(
                request.email(), request.mobileNumber(), tenantId, deviceInfo);
        var result = startUseCase.start(domainRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(new GuestInitiateResponse(
                result.workflowId(),
                result.status(),
                result.currentStep().name(),
                GuestStepInfo.nextAction(result.currentStep()),
                result.mobileOtpRequestId(),
                result.emailOtpRequestId(),
                result.maskedMobile(),
                result.maskedEmail(),
                false,
                result.failureReason(),
                Instant.now().toString()));
    }

    // ==================== Step 2: Verify dual OTP ====================

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify mobile + email OTP",
            description = "Verifies both OTPs in one call. Creates Keycloak user, returns JWT.",
            tags = "2. Guest — OTP")
    public ResponseEntity<GuestVerifyOtpResponse> verifyOtp(
            @Valid @RequestBody GuestVerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = verifyOtpUseCase.verify(workflowId, request.mobileOtp(),
                request.emailOtp(), device(httpRequest));
        // OTP retries keep workflow at OTP_SENT; only failureReason==null means both
        // OTPs verified and workflow advanced to OTP_VERIFIED.
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new GuestVerifyOtpResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                GuestStepInfo.nextAction(result.currentStep()),
                result.keycloakUserId(),
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn(),
                result.tokenType(),
                result.onboardingComplete(),
                result.failureReason(),
                Instant.now().toString()));
    }

    // ==================== Step 3: Enable biometrics (skippable) ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/enable-biometrics")
    @Operation(summary = "Enable Face ID / Touch ID (can be skipped)",
            tags = "3. Guest — Biometrics",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<GuestStepResponse> enableBiometrics(
            @Valid @RequestBody GuestBiometricsRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = biometricsUseCase.submit(workflowId, request.enabled(),
                request.skipped(), device(httpRequest));
        return ResponseEntity.ok(new GuestStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                GuestStepInfo.nextAction(result.currentStep()),
                false,
                result.message(),
                null,
                Instant.now().toString()));
    }

    // ==================== Step 4: Set PIN → COMPLETED ====================

    @SecuredEndpoint(obj = "onboarding", act = "update")
    @PostMapping("/set-pin")
    @Operation(summary = "Set 6-digit app PIN — completes guest journey (full KYC still pending)",
            tags = "4. Guest — PIN",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<GuestStepResponse> setPin(
            @Valid @RequestBody GuestSetPinRequest request,
            HttpServletRequest httpRequest) {
        String workflowId = workflowClient.workflowIdFor(request.email());
        var result = setPinUseCase.setPin(workflowId, request.pin(),
                request.confirmPin(), device(httpRequest));
        HttpStatus status = result.failureReason() != null
                ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        return ResponseEntity.status(status).body(new GuestStepResponse(
                result.workflowId(),
                result.currentStep().name(),
                result.currentStep().name(),
                GuestStepInfo.nextAction(result.currentStep()),
                result.onboardingComplete(),
                result.message(),
                result.failureReason(),
                Instant.now().toString()));
    }

    // ==================== Status ====================

    @GetMapping("/status")
    @Operation(summary = "Get guest onboarding state by email",
            tags = "5. Guest — Status")
    public ResponseEntity<GuestStatusResponse> status(@RequestParam String email) {
        GuestOnboardingState s = statusUseCase.getStatusByEmail(email);
        return ResponseEntity.ok(new GuestStatusResponse(
                s.getWorkflowId(),
                s.getCurrentStep() != null ? s.getCurrentStep().name() : null,
                s.getCurrentStep() != null ? s.getCurrentStep().name() : null,
                GuestStepInfo.nextAction(s.getCurrentStep()),
                s.getEmail(),
                s.getMobileNumber(),
                s.getKeycloakUserId(),
                s.isBiometricsEnabled(),
                s.isBiometricsSkipped(),
                s.isPinSet(),
                s.isOnboardingComplete(),
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
