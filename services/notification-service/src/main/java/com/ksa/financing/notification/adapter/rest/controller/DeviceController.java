package com.ksa.financing.notification.adapter.rest.controller;

import com.ksa.financing.notification.infrastructure.external.IdentityServiceClient;
import com.ksa.financing.notification.infrastructure.external.NovuClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final NovuClient novuClient;
    private final IdentityServiceClient identityServiceClient;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        // Mobile app sends only {customerId, fcmToken}. We resolve the Novu push integration
        // (fcm vs firebase-cloud-messaging-for-sullis) server-side by reading the
        // Keycloak onboarding_flow attribute via identity-service.
        String providerId = identityServiceClient.resolveNotificationProvider(request.customerId());
        log.info("Device register customerId={} resolved providerId={}", request.customerId(), providerId);

        novuClient.setSubscriberCredentials(request.customerId(), request.fcmToken(), providerId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Device token registered successfully"
        ));
    }

    public record DeviceRegistrationRequest(
            @NotBlank(message = "Customer ID is required") String customerId,
            @NotBlank(message = "FCM Token is required") String fcmToken
    ) {}
}
