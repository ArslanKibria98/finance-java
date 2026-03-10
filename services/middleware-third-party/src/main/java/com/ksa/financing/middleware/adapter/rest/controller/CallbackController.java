package com.ksa.financing.middleware.adapter.rest.controller;

import com.ksa.financing.middleware.application.dto.CallbackResponseDto;
import com.ksa.financing.middleware.domain.port.in.ManageCallbackUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
public class CallbackController {

    private final ManageCallbackUseCase manageCallbackUseCase;

    @SecuredEndpoint(obj = "middleware.callbacks", act = "manage")
    @GetMapping("/{id}")
    public ResponseEntity<CallbackResponseDto> getById(@PathVariable UUID id,
                                                        @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageCallbackUseCase.getById(tenantId, id));
    }

    @SecuredEndpoint(obj = "middleware.callbacks", act = "manage")
    @GetMapping
    public ResponseEntity<List<CallbackResponseDto>> listAll(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(manageCallbackUseCase.listAll(tenantId, page, size));
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaim("tenant_id");
        if (tenantClaim == null) {
            throw new com.ksa.financing.infra.exception.BusinessException(
                    "COMMON.AUTH.INVALID_CREDENTIALS",
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim.toString());
    }
}
