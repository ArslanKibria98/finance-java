package com.ksa.financing.notification.adapter.rest.controller;

import com.ksa.financing.notification.domain.model.TemplateRoutingRule;
import com.ksa.financing.notification.domain.repository.TemplateRoutingRuleRepository;
import com.ksa.financing.notification.infrastructure.external.NovuClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class NotificationRuleController {

    private final TemplateRoutingRuleRepository ruleRepository;
    private final NovuClient novuClient;
    private final com.ksa.financing.notification.application.service.NotificationOrchestrator notificationOrchestrator;

    @GetMapping("/rules")
    public ResponseEntity<List<TemplateRoutingRule>> getAllRules() {
        return ResponseEntity.ok(ruleRepository.findAll());
    }

    @PostMapping("/rules")
    public ResponseEntity<TemplateRoutingRule> createRule(@Valid @RequestBody NotificationRuleRequest request) {
        log.info("Creating new notification rule: {}", request.ruleCode());

        TemplateRoutingRule rule = TemplateRoutingRule.builder()
                .tenantId(UUID.fromString(request.tenantId()))
                .ruleCode(request.ruleCode())
                .ruleName(request.ruleName())
                .eventType(request.eventType())
                .channel(TemplateRoutingRule.NotificationChannel.valueOf(request.channel().toUpperCase()))
                .novuTemplateId(request.novuTemplateId())
                .priority(TemplateRoutingRule.Priority.valueOf(request.priority().toUpperCase()))
                .active(true)
                .build();

        return ResponseEntity.ok(ruleRepository.save(rule));
    }

    @GetMapping("/event-types")
    public ResponseEntity<List<String>> getEventTypes() {
        // These are the events currently supported by the system
        return ResponseEntity.ok(List.of(
                "PAYMENT_COMPLETED",
                "LOAN_APPROVED",
                "LOAN_DISBURSED",
                "AUTH_OTP",
                "DUNNING_STEP",
                "FRAUD_CHECK_FAILED",
                "SUSPICIOUS_ACTIVITY"
        ));
    }

    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getTemplates() {
        return ResponseEntity.ok(novuClient.getWorkflows());
    }

    @PostMapping("/trigger-test")
    public ResponseEntity<Map<String, Object>> triggerTest(@RequestBody Map<String, Object> payload) {
        String eventType = (String) payload.get("eventType");
        log.info("Manually triggering test event: {}", eventType);
        
        notificationOrchestrator.processEvent(eventType, payload);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Test notification triggered for event: " + eventType
        ));
    }

    public record NotificationRuleRequest(
            @NotBlank String tenantId,
            @NotBlank String ruleCode,
            @NotBlank String ruleName,
            @NotBlank String eventType,
            @NotBlank String channel,
            @NotBlank String novuTemplateId,
            @NotBlank String priority
    ) {}
}
