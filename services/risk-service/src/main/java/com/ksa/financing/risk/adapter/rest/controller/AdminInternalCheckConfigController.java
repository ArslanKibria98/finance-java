package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.InternalCheckConfig;
import com.ksa.financing.risk.domain.port.in.ManageInternalCheckConfigUseCase;
import com.ksa.financing.infra.security.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/risk/internal-checks/configs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Internal Checks Config", description = "Management of onboarding internal risk checks")
public class AdminInternalCheckConfigController {

    private final ManageInternalCheckConfigUseCase useCase;

    @GetMapping
    @Operation(summary = "List all internal check configurations")
    public List<InternalCheckConfig> list(HttpServletRequest request) {
        UUID tenantId = extractTenantId(request);
        return useCase.listConfigs(tenantId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update internal check configuration (toggle status/block code)")
    public InternalCheckConfig update(@PathVariable UUID id,
                                     @Valid @RequestBody ManageInternalCheckConfigUseCase.UpdateConfigCommand command,
                                     HttpServletRequest request) {
        UUID tenantId = extractTenantId(request);
        log.info("Updating internal check config id={} active={} for tenant={}", id, command.active(), tenantId);
        return useCase.updateConfig(tenantId, id, command);
    }

    private UUID extractTenantId(HttpServletRequest request) {
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null) {
            return UUID.fromString(tenantHeader);
        }
        String contextTenant = TenantContextHolder.getTenantId();
        if (contextTenant != null) {
            return UUID.fromString(contextTenant);
        }
        return UUID.randomUUID();
    }
}
