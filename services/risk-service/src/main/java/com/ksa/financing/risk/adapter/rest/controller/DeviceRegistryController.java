package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.risk.adapter.rest.request.BlockDeviceRequest;
import com.ksa.financing.risk.adapter.rest.response.BlockedDeviceResponse;
import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;
import com.ksa.financing.risk.domain.port.in.ManageDeviceRegistryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/risk/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Registry Management", description = "Admin APIs for managing device registry - block, unblock, remove devices")
public class DeviceRegistryController {

    private final ManageDeviceRegistryUseCase manageDeviceRegistryUseCase;

    @SecuredEndpoint(obj = "risk.devices", act = "create")
    @PostMapping("/block")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Block a device", description = "Blocks a device so it cannot pass internal checks")
    public DeviceRegistryEntry blockDevice(
            @Valid @RequestBody BlockDeviceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Blocking device: {}... for tenant: {}", maskDeviceId(request.deviceId()), tenantId);
        return manageDeviceRegistryUseCase.blockDevice(request.deviceId(), request.reason());
    }

    @SecuredEndpoint(obj = "risk.devices", act = "update")
    @PostMapping("/{deviceId}/unblock")
    @Operation(summary = "Unblock a device", description = "Removes block from a device")
    public DeviceRegistryEntry unblockDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Unblocking device: {}... for tenant: {}", maskDeviceId(deviceId), tenantId);
        return manageDeviceRegistryUseCase.unblockDevice(deviceId);
    }

    @SecuredEndpoint(obj = "risk.devices", act = "read")
    @GetMapping("/{deviceId}/status")
    @Operation(summary = "Get device status", description = "Returns device registry entry with block status")
    public DeviceRegistryEntry getDeviceStatus(
            @PathVariable String deviceId,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        return manageDeviceRegistryUseCase.getDeviceStatus(deviceId);
    }

    @SecuredEndpoint(obj = "risk.devices", act = "read")
    @GetMapping("/blocked")
    @Operation(summary = "List all blocked devices", description = "Returns devices blocked by admin OR flagged by internal checks (identity farming). Grouped by deviceId with all NID associations.")
    public List<BlockedDeviceResponse> listBlockedDevices(@AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        var entries = manageDeviceRegistryUseCase.listBlockedDevices();
        return groupByDevice(entries);
    }

    private List<BlockedDeviceResponse> groupByDevice(List<DeviceRegistryEntry> entries) {
        Map<String, List<DeviceRegistryEntry>> grouped = entries.stream()
                .collect(Collectors.groupingBy(DeviceRegistryEntry::deviceId, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream().map(entry -> {
            String deviceId = entry.getKey();
            var rows = entry.getValue();
            var first = rows.getFirst();

            boolean adminBlocked = rows.stream().anyMatch(DeviceRegistryEntry::blocked);
            String blockReason = rows.stream()
                    .map(DeviceRegistryEntry::blockReason)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
            String blockSource = rows.stream()
                    .map(DeviceRegistryEntry::blockSource)
                    .filter(Objects::nonNull)
                    .findFirst().orElse("UNKNOWN");

            int totalNids = rows.size();
            int totalAttempts = rows.stream().mapToInt(DeviceRegistryEntry::attemptCount).sum();

            var earliestSeen = rows.stream()
                    .map(DeviceRegistryEntry::firstSeenAt).filter(Objects::nonNull)
                    .min(java.time.Instant::compareTo).orElse(null);
            var latestSeen = rows.stream()
                    .map(DeviceRegistryEntry::lastSeenAt).filter(Objects::nonNull)
                    .max(java.time.Instant::compareTo).orElse(null);

            var nidAssociations = rows.stream()
                    .map(r -> new BlockedDeviceResponse.NidAssociation(
                            r.nidHash(), r.attemptCount(), r.firstSeenAt(), r.lastSeenAt()))
                    .toList();

            String blockType = switch (blockSource) {
                case "ADMIN_BLOCKED", "IDENTITY_FARMING" -> "HARD_BLOCK";
                case "VELOCITY_EXCEEDED" -> "SOFT_BLOCK";
                default -> "UNKNOWN";
            };

            return new BlockedDeviceResponse(
                    deviceId, first.deviceFingerprint(),
                    adminBlocked, blockReason, blockSource, blockType,
                    totalNids, totalAttempts,
                    earliestSeen, latestSeen,
                    nidAssociations);
        }).toList();
    }

    @SecuredEndpoint(obj = "risk.devices", act = "read")
    @GetMapping
    @Operation(summary = "List all devices in registry")
    public List<DeviceRegistryEntry> listAllDevices(@AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        return manageDeviceRegistryUseCase.listAllDevices();
    }

    @SecuredEndpoint(obj = "risk.devices", act = "delete")
    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove device from registry", description = "Permanently removes all entries for this device")
    public void removeDevice(
            @PathVariable String deviceId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Removing device: {}... for tenant: {}", maskDeviceId(deviceId), tenantId);
        manageDeviceRegistryUseCase.removeDevice(deviceId);
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() < 8) return "****";
        return deviceId.substring(0, 8) + "...";
    }
}
