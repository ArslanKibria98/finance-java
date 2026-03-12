package com.ksa.financing.fraud.adapter.rest.controller;

import com.ksa.financing.fraud.application.dto.FraudHistoryCheckRequestDto;
import com.ksa.financing.fraud.application.dto.FraudHistoryCheckResponseDto;
import com.ksa.financing.fraud.domain.port.in.CheckFraudHistoryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal endpoint for risk-service to check fraud history.
 * Public (no JWT) — called service-to-service via X-Tenant-Id header.
 */
@RestController
@RequestMapping("/api/v1/fraud/history-check")
@RequiredArgsConstructor
@Slf4j
public class FraudHistoryCheckController {

    private final CheckFraudHistoryUseCase checkFraudHistoryUseCase;

    @PostMapping
    public ResponseEntity<FraudHistoryCheckResponseDto> checkFraudHistory(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
            @RequestBody FraudHistoryCheckRequestDto request) {

        var tenantId = tenantIdHeader != null ? UUID.fromString(tenantIdHeader) : UUID.fromString("00000000-0000-0000-0000-000000000001");

        log.info("Fraud history check request: tenantId={} customerId={}", tenantId, request.customerId());

        var result = checkFraudHistoryUseCase.checkHistory(
                tenantId,
                request.nationalIdHash(),
                request.mobileHash(),
                request.customerId()
        );

        var response = new FraudHistoryCheckResponseDto(
                result.hasFraudHistory(),
                result.confirmedFraudCount(),
                result.suspectedFraudCount(),
                result.totalEvaluations(),
                result.riskLevel(),
                result.recommendation()
        );

        return ResponseEntity.ok(response);
    }
}
