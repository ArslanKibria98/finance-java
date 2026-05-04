package com.ksa.financing.collections.adapter.rest.controller;

import com.ksa.financing.collections.application.service.OverdueDetectionService;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Operator endpoint to trigger the overdue detection job on demand.
 * Primary scheduling happens via {@code @Scheduled} inside {@link OverdueDetectionService}.
 */
@RestController
@RequestMapping("/api/v1/jobs/overdue-detection")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Jobs", description = "Operator-triggered maintenance jobs")
public class OverdueJobController {

    private final OverdueDetectionService overdueDetectionService;

    @PostMapping("/run")
    @Operation(summary = "Manually trigger the overdue detection scan")
    @SecuredEndpoint(obj = "jobs.overdue-detection", act = "run")
    public ResponseEntity<Map<String, Object>> run(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        var targetDate = asOf != null ? asOf : LocalDate.now();
        int overdueCount = overdueDetectionService.scan(targetDate);
        return ResponseEntity.ok(Map.of(
                "asOf", targetDate,
                "schedulesWithOverdue", overdueCount
        ));
    }
}
