package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.model.BlockCode;
import com.ksa.financing.risk.domain.port.in.ManageBlockCodeUseCase;
import com.ksa.financing.infra.security.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/risk/block-codes")
@RequiredArgsConstructor
@Tag(name = "Admin - Block Codes", description = "Management of standardized block reason codes")
public class AdminBlockCodeController {

    private final ManageBlockCodeUseCase manageBlockCodeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add new block code")
    public BlockCode create(@Valid @RequestBody ManageBlockCodeUseCase.CreateBlockCodeCommand command,
                            HttpServletRequest request) {
        UUID tenantId = extractTenantId(request);
        return manageBlockCodeUseCase.createBlockCode(tenantId, command);
    }

    @GetMapping
    @Operation(summary = "List all block codes")
    public List<BlockCode> list(HttpServletRequest request) {
        UUID tenantId = extractTenantId(request);
        return manageBlockCodeUseCase.listBlockCodes(tenantId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update block code")
    public BlockCode update(@PathVariable UUID id,
                            @Valid @RequestBody ManageBlockCodeUseCase.UpdateBlockCodeCommand command,
                            HttpServletRequest request) {
        UUID tenantId = extractTenantId(request);
        return manageBlockCodeUseCase.updateBlockCode(tenantId, id, command);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete block code")
    public void delete(@PathVariable UUID id, HttpServletRequest request) {
        UUID tenantId = extractTenantId(request);
        manageBlockCodeUseCase.deleteBlockCode(tenantId, id);
    }

    private UUID extractTenantId(HttpServletRequest request) {
        // Fallback to TenantContextHolder if header is missing (internal calls)
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null) {
            return UUID.fromString(tenantHeader);
        }
        String contextTenant = TenantContextHolder.getTenantId();
        if (contextTenant != null) {
            return UUID.fromString(contextTenant);
        }
        return UUID.randomUUID(); // Simplified for now
    }
}
