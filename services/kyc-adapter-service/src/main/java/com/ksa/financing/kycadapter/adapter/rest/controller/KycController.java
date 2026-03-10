package com.ksa.financing.kycadapter.adapter.rest.controller;

import com.ksa.financing.kycadapter.application.dto.*;
import com.ksa.financing.kycadapter.application.mapper.KycMapper;
import com.ksa.financing.kycadapter.domain.port.in.*;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for KYC verification operations.
 * Provides endpoints for mobile verification (Tahakuk), national ID verification (Nafath/Yakeen),
 * AML/sanctions screening, and salary verification (GOSI).
 */
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC Verification", description = "KYC verification operations for Saudi government APIs")
public class KycController {

    private static final Logger log = LoggerFactory.getLogger(KycController.class);

    private final VerifyMobileUseCase verifyMobileUseCase;
    private final InitiateNafathUseCase initiateNafathUseCase;
    private final VerifyIdentityUseCase verifyIdentityUseCase;
    private final ScreenSanctionsUseCase screenSanctionsUseCase;
    private final FetchSalaryUseCase fetchSalaryUseCase;
    private final SendOtpUseCase sendOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final ResendOtpUseCase resendOtpUseCase;

    /**
     * Verify mobile ownership via Tahakuk.
     */
    @SecuredEndpoint(obj = "kyc.tahakuk", act = "verify")
    @PostMapping("/tahakuk/verify")
    @Operation(summary = "Verify mobile ownership", description = "Mobile ownership verification via Tahakuk government service")
    public ResponseEntity<VerificationSessionResponse> verifyMobile(
            @Valid @RequestBody VerifyMobileRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Tahakuk mobile verification requested for tenant={}", tenantId);

        var command = new VerifyMobileUseCase.VerifyMobileCommand(
                tenantId,
                request.mobileNumber(),
                request.nationalId(),
                UUID.randomUUID().toString()
        );

        var session = verifyMobileUseCase.verify(command);
        return ResponseEntity.ok(KycMapper.toResponse(session));
    }

    /**
     * Initiate Nafath session for national ID verification.
     */
    @SecuredEndpoint(obj = "kyc.nafath", act = "initiate")
    @PostMapping("/nafath/initiate")
    @Operation(summary = "Initiate Nafath session", description = "Starts Nafath identity verification session, returns random number for user to confirm")
    public ResponseEntity<NafathInitiateResponse> initiateNafath(
            @Valid @RequestBody NafathInitiateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Nafath session initiation requested for tenant={}", tenantId);

        var command = new InitiateNafathUseCase.InitiateNafathCommand(
                tenantId,
                request.nationalId(),
                UUID.randomUUID().toString()
        );

        var result = initiateNafathUseCase.initiate(command);
        var response = new NafathInitiateResponse(
                result.session().getId(),
                result.randomNumber(),
                result.transactionId(),
                result.session().getStatus().name(),
                result.nafathVerificationData()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Check Nafath session status.
     */
    @SecuredEndpoint(obj = "kyc.nafath", act = "status")
    @GetMapping("/nafath/status/{sessionId}")
    @Operation(summary = "Check Nafath session status", description = "Returns current status of a Nafath verification session")
    public ResponseEntity<VerificationSessionResponse> checkNafathStatus(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Nafath status check for session={}, tenant={}", sessionId, tenantId);

        var session = initiateNafathUseCase.checkStatus(sessionId);
        return ResponseEntity.ok(KycMapper.toResponse(session));
    }

    /**
     * Verify identity via Yakeen.
     */
    @SecuredEndpoint(obj = "kyc.yakeen", act = "verify")
    @PostMapping("/yakeen/verify")
    @Operation(summary = "Verify identity via Yakeen", description = "Identity verification using Yakeen government service with demographics lookup")
    public ResponseEntity<YakeenVerifyResponse> verifyIdentity(
            @Valid @RequestBody YakeenVerifyRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Yakeen identity verification requested for tenant={}", tenantId);

        var command = new VerifyIdentityUseCase.VerifyIdentityCommand(
                tenantId,
                request.nationalId(),
                request.dateOfBirth(),
                UUID.randomUUID().toString()
        );

        var result = verifyIdentityUseCase.verify(command);
        var response = new YakeenVerifyResponse(
                result.session().getId(),
                result.session().getResult() != null ? result.session().getResult().name() : null,
                result.demographics()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * AML/Sanctions screening check.
     */
    @SecuredEndpoint(obj = "kyc.screening", act = "check")
    @PostMapping("/screening/check")
    @Operation(summary = "AML/Sanctions screening", description = "Screen individual against sanctions lists (UN, SAMA, local PEP)")
    public ResponseEntity<SanctionsScreenResponse> screenSanctions(
            @Valid @RequestBody SanctionsScreenRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Sanctions screening requested for tenant={}", tenantId);

        var command = new ScreenSanctionsUseCase.ScreenSanctionsCommand(
                tenantId,
                request.fullName(),
                request.nationalId(),
                request.nationality(),
                UUID.randomUUID().toString()
        );

        var result = screenSanctionsUseCase.screen(command);
        var response = new SanctionsScreenResponse(
                result.session().getId(),
                result.screeningStatus(),
                result.hit()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch salary information from GOSI.
     */
    @SecuredEndpoint(obj = "kyc.gosi", act = "fetch")
    @PostMapping("/gosi/fetch")
    @Operation(summary = "Fetch salary from GOSI", description = "Retrieve employment and salary information from GOSI government service")
    public ResponseEntity<GosiSalaryResponse> fetchSalary(
            @Valid @RequestBody GosiSalaryRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("GOSI salary fetch requested for tenant={}", tenantId);

        var command = new FetchSalaryUseCase.FetchSalaryCommand(
                tenantId,
                request.nationalId(),
                UUID.randomUUID().toString()
        );

        var result = fetchSalaryUseCase.fetch(command);
        var response = new GosiSalaryResponse(
                result.employerName(),
                result.basicSalary(),
                result.housingAllowance(),
                result.totalSalary(),
                result.verificationSource()
        );
        return ResponseEntity.ok(response);
    }

    // ==================== OTP Endpoints (Unifonic) ====================

    /**
     * Send OTP to mobile number via Unifonic.
     * This endpoint is public (no JWT required) as it is called by onboarding workflow activities.
     */
    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP", description = "Send a one-time password to the specified mobile number via Unifonic")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("OTP send requested for nationalId ending in ...{}",
                request.nationalId() != null && request.nationalId().length() >= 4
                        ? request.nationalId().substring(request.nationalId().length() - 4)
                        : "***");

        var command = new SendOtpUseCase.SendOtpCommand(
                request.nationalId(),
                request.mobileNumber(),
                UUID.randomUUID().toString()
        );

        var result = sendOtpUseCase.send(command);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("otpRequestId", result.otpRequestId());
        response.put("sent", result.sent());
        response.put("maskedMobile", result.maskedMobile());

        return ResponseEntity.ok(response);
    }

    /**
     * Verify OTP code via Unifonic.
     * This endpoint is public (no JWT required) as it is called by onboarding workflow activities.
     */
    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verify a one-time password code against a previously sent OTP request")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("OTP verification requested for otpRequestId={}", request.otpRequestId());

        var command = new VerifyOtpUseCase.VerifyOtpCommand(
                request.nationalId(),
                request.otpCode(),
                request.otpRequestId()
        );

        var result = verifyOtpUseCase.verify(command);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("verified", result.verified());
        response.put("failureReason", result.failureReason());

        return ResponseEntity.ok(response);
    }

    /**
     * Resend OTP via Unifonic.
     * This endpoint is public (no JWT required) as it is called by onboarding workflow activities.
     */
    @PostMapping("/otp/resend")
    @Operation(summary = "Resend OTP", description = "Resend a one-time password, generating a new code for an existing OTP request")
    public ResponseEntity<Map<String, Object>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("OTP resend requested for otpRequestId={}", request.otpRequestId());

        var command = new ResendOtpUseCase.ResendOtpCommand(
                request.nationalId(),
                request.mobileNumber(),
                request.otpRequestId()
        );

        var result = resendOtpUseCase.resend(command);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("otpRequestId", result.newOtpRequestId());
        response.put("sent", result.sent());
        response.put("remainingAttempts", result.remainingAttempts());

        return ResponseEntity.ok(response);
    }

    /**
     * Extract tenant ID from JWT token's tenant_id claim.
     * Throws BusinessException if tenant_id is missing from the JWT.
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
}
