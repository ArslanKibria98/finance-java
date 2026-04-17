package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.application.dto.StartInternalCheckRequestDto;
import com.ksa.financing.risk.domain.model.InternalCheckRequest;
import com.ksa.financing.risk.domain.model.InternalCheckResult;
import com.ksa.financing.risk.domain.port.in.RunInternalChecksUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Assessment", description = "Phase 1 Internal Checks - Fraud, AML, Sanctions")
public class InternalChecksController {

    private final RunInternalChecksUseCase runInternalChecksUseCase;

    // Country-wise NID validation patterns
    private static final Map<String, NidFormat> NID_FORMATS = Map.of(
            "SAU", new NidFormat("^[12]\\d{9}$", 10, "Saudi NID must be 10 digits starting with 1 (citizen) or 2 (resident)"),
            "PAK", new NidFormat("^\\d{13}$", 13, "Pakistan CNIC must be 13 digits"),
            "ARE", new NidFormat("^784\\d{12}$", 15, "Emirates ID must be 15 digits starting with 784")
    );

    private record NidFormat(String regex, int length, String errorMessage) {}

    @PostMapping("/internal-checks")
    @Operation(summary = "Run internal checks",
        description = "Runs all 10 Phase 1 internal risk checks synchronously. "
            + "Supports 3 input modes: NID only, mobile only, or both. "
            + "countryCode controls NID validation (SAU/PAK/ARE). Defaults to SAU.")
    public InternalCheckResult runInternalChecks(
            @Valid @RequestBody StartInternalCheckRequestDto request,
            HttpServletRequest httpRequest) {

        // At least one identifier must be provided
        if (!request.hasNationalId() && !request.hasMobileNumber()) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    "At least one of nationalId or mobileNumber must be provided");
        }

        String countryCode = request.resolvedCountryCode();

        // Country-wise NID validation (only if NID is provided)
        if (request.hasNationalId()) {
            validateNationalId(request.nationalId(), countryCode);
        }

        UUID tenantId = extractTenantId(httpRequest);

        String identifier = request.hasNationalId()
                ? "NID:" + maskNid(request.nationalId())
                : "Mobile:" + maskMobile(request.mobileNumber());
        log.info("Running internal checks for {} country={} tenant={}", identifier, countryCode, tenantId);

        var domainRequest = new InternalCheckRequest(
            request.hasNationalId() ? request.nationalId() : null,
            request.hasNationalId() ? hashValue(request.nationalId()) : null,
            request.hasMobileNumber() ? request.mobileNumber() : null,
            request.hasMobileNumber() ? hashValue(request.mobileNumber()) : null,
            httpRequest.getHeader("X-Device-Id"),
            httpRequest.getHeader("X-Device-Fingerprint"),
            httpRequest.getHeader("X-Client-Ip"),
            httpRequest.getHeader("X-Session-Id"),
            tenantId.toString(),
            countryCode
        );

        var result = runInternalChecksUseCase.run(domainRequest);
        log.info("Internal checks completed: assessmentId={}, status={}, decision={}",
            result.assessmentId(), result.status(), result.overallDecision());

        return result;
    }

    /**
     * Validate NID format based on country code.
     */
    private void validateNationalId(String nationalId, String countryCode) {
        String cleanNid = nationalId.replaceAll("[\\s\\-]", "");

        NidFormat format = NID_FORMATS.get(countryCode);
        if (format == null) {
            // Unknown country — only validate it's not empty
            log.warn("No NID format defined for country: {}, skipping format validation", countryCode);
            return;
        }

        if (!Pattern.matches(format.regex(), cleanNid)) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_FAILED,
                    format.errorMessage() + ". Got: " + cleanNid.length() + " digits");
        }
    }

    private String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(
                ErrorCodes.TECHNICAL_ERROR,
                "SHA-256 algorithm not available", e);
        }
    }

    private String maskNid(String nid) {
        if (nid == null || nid.length() < 4) return "****";
        return "****" + nid.substring(nid.length() - 4);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }

    private UUID extractTenantId(HttpServletRequest request) {
        var tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader == null || tenantHeader.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "X-Tenant-Id header is required");
        }
        return UUID.fromString(tenantHeader);
    }
}
