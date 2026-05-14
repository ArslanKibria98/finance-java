package com.ksa.financing.lending.adapter.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.infrastructure.persistence.entity.RescheduleConfigJpaEntity;
import com.ksa.financing.lending.infrastructure.persistence.repository.JpaRescheduleConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reschedule-configs")
@RequiredArgsConstructor
@Tag(name = "Admin - Reschedule Configuration", description = "Admin APIs to manage global reschedule rules and metadata")
public class AdminRescheduleConfigController {

    private final JpaRescheduleConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    @SecuredEndpoint(obj = "admin.reschedule-configs", act = "read")
    @Operation(summary = "List all global reschedule configurations (filterable by free-text ?search=)")
    public ResponseEntity<List<RescheduleConfigJpaEntity>> listConfigs(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = extractTenantId(jwt);
        var configs = configRepository.findByTenantIdAndActiveTrue(UUID.fromString(tenantId));
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            configs = configs.stream().filter(c -> matchesSearch(c, term)).toList();
        }
        return ResponseEntity.ok(configs);
    }

    private boolean matchesSearch(RescheduleConfigJpaEntity c, String term) {
        return contains(c.getRescheduleType(), term)
                || contains(c.getLabelEn(), term)
                || contains(c.getLabelAr(), term)
                || contains(c.getDescriptionEn(), term)
                || contains(c.getDescriptionAr(), term);
    }

    private boolean contains(String f, String term) {
        return f != null && f.toLowerCase().contains(term);
    }

    @PutMapping("/{type}")
    @SecuredEndpoint(obj = "admin.reschedule-configs", act = "update")
    @Operation(summary = "Update global reschedule configuration for a specific type")
    public ResponseEntity<RescheduleConfigJpaEntity> updateConfig(
            @PathVariable String type,
            @RequestBody RescheduleConfigRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = extractTenantId(jwt);
        var config = configRepository.findByTenantIdAndRescheduleType(UUID.fromString(tenantId), type)
                .orElseThrow(() -> NotFoundException.forEntity("RescheduleConfig", type));

        config.setLabelEn(request.labelEn());
        config.setLabelAr(request.labelAr());
        config.setDescriptionEn(request.descriptionEn());
        config.setDescriptionAr(request.descriptionAr());
        config.setRequiresApproval(request.requiresApproval());
        config.setApproverRole(request.approverRole());
        
        if (request.fieldsConfig() != null) {
            config.setFieldsConfig(request.fieldsConfig().toString());
        }
        if (request.rulesConfig() != null) {
            config.setRulesConfig(request.rulesConfig().toString());
        }

        return ResponseEntity.ok(configRepository.save(config));
    }

    private String extractTenantId(Jwt jwt) {
        return jwt.getClaimAsString("tenantId") != null ? jwt.getClaimAsString("tenantId") : "00000000-0000-0000-0000-000000000001";
    }

    public record RescheduleConfigRequest(
            String labelEn,
            String labelAr,
            String descriptionEn,
            String descriptionAr,
            boolean requiresApproval,
            String approverRole,
            JsonNode fieldsConfig,
            JsonNode rulesConfig
    ) {}
}
