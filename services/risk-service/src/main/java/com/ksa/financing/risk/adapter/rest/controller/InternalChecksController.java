package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.domain.valueobject.NationalId;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Assessment", description = "Phase 1 Internal Checks - Fraud, AML, Sanctions")
public class InternalChecksController {

    private final RunInternalChecksUseCase runInternalChecksUseCase;

    @PostMapping("/internal-checks")
    @Operation(summary = "Run internal checks",
        description = "Runs all 10 Phase 1 internal risk checks synchronously and returns the result. Public API called during onboarding.")
    public InternalCheckResult runInternalChecks(
            @Valid @RequestBody StartInternalCheckRequestDto request,
            HttpServletRequest httpRequest) {

        // Validate NID format (10 digits, starts with 1=citizen or 2=resident)
        NationalId.of(request.nationalId());

        UUID tenantId = extractTenantId(httpRequest);
        log.info("Running internal checks for NID ending ...{} for tenant: {}", maskNid(request.nationalId()), tenantId);

        var domainRequest = new InternalCheckRequest(
            request.nationalId(),
            hashValue(request.nationalId()),
            request.mobileNumber(),
            hashValue(request.mobileNumber()),
            httpRequest.getHeader("X-Device-Id"),
            httpRequest.getHeader("X-Device-Fingerprint"),
            httpRequest.getHeader("X-Client-Ip"),
            httpRequest.getHeader("X-Session-Id"),
            tenantId.toString()
        );

        var result = runInternalChecksUseCase.run(domainRequest);
        log.info("Internal checks completed: assessmentId={}, status={}, decision={}",
            result.assessmentId(), result.status(), result.overallDecision());

        return result;
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
