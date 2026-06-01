package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.risk.domain.port.in.PerformCreditCheckUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credit-check")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Credit Check", description = "SIMAH credit check endpoint for service-to-service calls")
public class CreditCheckController {

    private final PerformCreditCheckUseCase performCreditCheckUseCase;

    @PostMapping
    @Operation(summary = "Perform credit check", description = "Calls SIMAH via middleware and returns credit data. Public endpoint for service-to-service calls.")
    public ResponseEntity<Map<String, Object>> performCreditCheck(@RequestBody Map<String, Object> request) {
        log.info("Credit check request received");

        var tenantId = request.containsKey("tenantId") ? UUID.fromString(request.get("tenantId").toString()) : null;
        var nationalId = request.containsKey("nationalId") ? request.get("nationalId").toString() : null;
        var customerId = request.containsKey("customerId") ? request.get("customerId").toString() : null;
        var applicationId = request.containsKey("applicationId") ? request.get("applicationId").toString() : null;
        var requestedAmount = request.containsKey("requestedAmount")
                ? new BigDecimal(request.get("requestedAmount").toString()) : BigDecimal.ZERO;

        if (nationalId == null || nationalId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "nationalId is required"));
        }

        var command = new PerformCreditCheckUseCase.CreditCheckCommand(
                tenantId, nationalId, customerId, applicationId, requestedAmount);

        var result = performCreditCheckUseCase.performCreditCheck(command);

        return ResponseEntity.ok(Map.of(
                "creditScore", result.creditScore(),
                "simahGrade", result.simahGrade(),
                "simahReferenceId", result.simahReferenceId(),
                "verifiedSalary", result.verifiedSalary(),
                "existingObligations", result.existingObligations(),
                "hasActiveDefaults", result.hasActiveDefaults(),
                "defaultsCount", result.defaultsCount(),
                "activeLoansCount", result.activeLoansCount()
        ));
    }
}
