package com.ksa.financing.service.template.adapter.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller providing service status information.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final BuildProperties buildProperties;

    @GetMapping
    @Operation(summary = "Health check endpoint")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Health check requested");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", buildProperties.getName());
        health.put("version", buildProperties.getVersion());
        health.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe for Kubernetes")
    @ApiResponse(responseCode = "200", description = "Service is ready")
    public ResponseEntity<Map<String, String>> ready() {
        // Check if service is ready to accept traffic
        // Could include checks for database connectivity, etc.
        Map<String, String> status = new HashMap<>();
        status.put("status", "READY");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness probe for Kubernetes")
    @ApiResponse(responseCode = "200", description = "Service is alive")
    public ResponseEntity<Map<String, String>> live() {
        // Simple liveness check
        Map<String, String> status = new HashMap<>();
        status.put("status", "ALIVE");
        return ResponseEntity.ok(status);
    }
}