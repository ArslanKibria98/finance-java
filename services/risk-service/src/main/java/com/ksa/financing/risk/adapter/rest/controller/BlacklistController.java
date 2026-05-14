package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.adapter.rest.request.BlacklistMobileRequest;
import com.ksa.financing.risk.adapter.rest.request.BlacklistNidRequest;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;
import com.ksa.financing.risk.domain.port.in.ManageBlacklistUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk/blacklist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Blacklist Management", description = "Admin APIs for blacklisting NID and mobile numbers")
public class BlacklistController {

    private final ManageBlacklistUseCase manageBlacklistUseCase;

    // ===== NID BLACKLIST =====

    @SecuredEndpoint(obj = "risk.blacklist", act = "create")
    @PostMapping("/nid")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Blacklist a National ID")
    public NidBlacklistEntry blacklistNid(
            @Valid @RequestBody BlacklistNidRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        // Validate NID format (10 digits, starts with 1=citizen or 2=resident)
        NationalId.of(request.nationalId());

        UUID tenantId = extractTenantId(jwt);
        log.info("Blacklisting NID: ****{} for tenant: {}", request.nationalId().substring(Math.max(0, request.nationalId().length() - 4)), tenantId);
        return manageBlacklistUseCase.blacklistNid(request.nationalId(), request.reason(), request.blockCodeId());
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "delete")
    @PostMapping("/nid/{nationalId}/remove")
    @Operation(summary = "Remove NID from blacklist", description = "Sets status to REMOVED")
    public NidBlacklistEntry removeNid(
            @PathVariable String nationalId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Removing NID from blacklist: ****{} for tenant: {}", nationalId.substring(Math.max(0, nationalId.length() - 4)), tenantId);
        return manageBlacklistUseCase.removeNid(nationalId);
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "check")
    @GetMapping("/nid/{nationalId}/status")
    @Operation(summary = "Check NID blacklist status")
    public NidBlacklistEntry getNidStatus(
            @PathVariable String nationalId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageBlacklistUseCase.getNidStatus(nationalId);
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "read")
    @GetMapping("/nid")
    @Operation(summary = "List all NID blacklist entries (paginated)")
    public PageResponse<NidBlacklistEntry> listNidBlacklist(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageBlacklistUseCase.listNidBlacklist(pageQuery);
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "update")
    @PutMapping("/nid/{nationalId}/block-code")
    @Operation(summary = "Assign block code to NID blacklist entry")
    public NidBlacklistEntry assignNidBlockCode(
            @PathVariable String nationalId,
            @Valid @RequestBody AssignBlockCodeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Assigning block code {} to NID ****{} for tenant: {}", request.blockCodeId(),
                nationalId.substring(Math.max(0, nationalId.length() - 4)), tenantId);
        return manageBlacklistUseCase.assignNidBlockCode(nationalId, request.blockCodeId());
    }

    // ===== MOBILE BLACKLIST =====

    @SecuredEndpoint(obj = "risk.blacklist", act = "create")
    @PostMapping("/mobile")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Blacklist a mobile number")
    public MobileBlacklistEntry blacklistMobile(
            @Valid @RequestBody BlacklistMobileRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Blacklisting mobile: ****{} for tenant: {}", request.mobileNumber().substring(Math.max(0, request.mobileNumber().length() - 4)), tenantId);
        return manageBlacklistUseCase.blacklistMobile(request.mobileNumber(), request.reason(), request.blockCodeId());
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "delete")
    @PostMapping("/mobile/{mobileNumber}/remove")
    @Operation(summary = "Remove mobile from blacklist", description = "Sets status to REMOVED")
    public MobileBlacklistEntry removeMobile(
            @PathVariable String mobileNumber,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Removing mobile from blacklist: ****{} for tenant: {}", mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)), tenantId);
        return manageBlacklistUseCase.removeMobile(mobileNumber);
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "check")
    @GetMapping("/mobile/{mobileNumber}/status")
    @Operation(summary = "Check mobile blacklist status")
    public MobileBlacklistEntry getMobileStatus(
            @PathVariable String mobileNumber,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageBlacklistUseCase.getMobileStatus(mobileNumber);
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "read")
    @GetMapping("/mobile")
    @Operation(summary = "List all mobile blacklist entries (paginated)")
    public PageResponse<MobileBlacklistEntry> listMobileBlacklist(
            PageQuery pageQuery,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return manageBlacklistUseCase.listMobileBlacklist(pageQuery);
    }

    @SecuredEndpoint(obj = "risk.blacklist", act = "update")
    @PutMapping("/mobile/{mobileNumber}/block-code")
    @Operation(summary = "Assign block code to mobile blacklist entry")
    public MobileBlacklistEntry assignMobileBlockCode(
            @PathVariable String mobileNumber,
            @Valid @RequestBody AssignBlockCodeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Assigning block code {} to mobile ****{} for tenant: {}", request.blockCodeId(),
                mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)), tenantId);
        return manageBlacklistUseCase.assignMobileBlockCode(mobileNumber, request.blockCodeId());
    }

    // ===== SHARED =====

    public record AssignBlockCodeRequest(UUID blockCodeId) {}

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
