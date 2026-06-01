package com.ksa.financing.notification.adapter.rest.controller;

import com.ksa.financing.notification.application.service.NotificationPreferenceService;
import com.ksa.financing.notification.domain.model.NotificationPreference;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping("/{customerId}")
    public ResponseEntity<PreferenceResponse> getPreference(
            @PathVariable UUID customerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId) {
        log.info("Fetching preference: tenantId={} customerId={}", tenantId, customerId);
        NotificationPreference prefs = preferenceService.getOrCreateDefault(tenantId, customerId);
        return ResponseEntity.ok(PreferenceResponse.from(prefs));
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<PreferenceResponse> updatePreference(
            @PathVariable UUID customerId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody UpdatePreferenceRequest request) {
        log.info("Updating preference: tenantId={} customerId={} language={}",
                tenantId, customerId, request.preferredLanguage());

        NotificationPreference prefs = preferenceService.updatePreference(
                tenantId,
                customerId,
                request.preferredLanguage(),
                request.smsEnabled(),
                request.emailEnabled(),
                request.pushEnabled()
        );
        return ResponseEntity.ok(PreferenceResponse.from(prefs));
    }

    public record UpdatePreferenceRequest(
            String preferredLanguage,
            Boolean smsEnabled,
            Boolean emailEnabled,
            Boolean pushEnabled
    ) {}

    public record PreferenceResponse(
            @NotNull UUID tenantId,
            @NotNull UUID customerId,
            String preferredLanguage,
            boolean smsEnabled,
            boolean emailEnabled,
            boolean pushEnabled
    ) {
        public static PreferenceResponse from(NotificationPreference prefs) {
            return new PreferenceResponse(
                    prefs.getTenantId(),
                    prefs.getCustomerId(),
                    prefs.getPreferredLanguage(),
                    prefs.isSmsEnabled(),
                    prefs.isEmailEnabled(),
                    prefs.isPushEnabled()
            );
        }
    }
}
