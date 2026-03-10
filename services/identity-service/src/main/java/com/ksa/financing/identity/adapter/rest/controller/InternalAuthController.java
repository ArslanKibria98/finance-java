package com.ksa.financing.identity.adapter.rest.controller;

import com.ksa.financing.identity.domain.port.in.CheckAuthorizationUseCase;
import com.ksa.financing.identity.domain.port.in.CheckAuthorizationUseCase.AuthorizationRequest;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Internal service-to-service authorization endpoint.
 * Called by other services when Redis cache is empty.
 *
 * <p>This endpoint is on /internal/** path which is:
 * - NOT protected by JWT (service-to-service trust within Docker network)
 * - NOT checked by Casbin filter (in skip patterns)
 * - Protected by network-level access (Docker internal network only)</p>
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
@Hidden // Hide from Swagger (internal-only)
public class InternalAuthController {

    private final CheckAuthorizationUseCase checkAuthorizationUseCase;

    @PostMapping("/authorize")
    public ResponseEntity<Map<String, Object>> authorize(@RequestBody Map<String, String> request) {
        String subject = request.get("subject");
        String resource = request.get("resource");
        String action = request.get("action");

        log.debug("Internal auth check: sub={}, obj={}, act={}", subject, resource, action);

        var result = checkAuthorizationUseCase.check(
                new AuthorizationRequest(subject, resource, action));

        return ResponseEntity.ok(Map.of(
                "allowed", result.allowed(),
                "subject", result.subject(),
                "resource", result.resource(),
                "action", result.action()
        ));
    }
}
