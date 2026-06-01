package com.ksa.financing.onboarding.shared.web;

import io.temporal.client.WorkflowNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Temporal-client signal/query failures (after a workflow has already
 * completed or failed) to a clean 422 response that tells the mobile app to
 * restart the onboarding journey, instead of bubbling up to the generic
 * 500 "Internal Server Error" returned by the foundational-infra global
 * exception handler.
 *
 * <p>This advice is ordered above the SDK's global handler so it intercepts
 * first.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class WorkflowExceptionAdvice {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWorkflowNotFound(WorkflowNotFoundException ex) {
        log.warn("Signal/query against missing workflow: {} — returning 422 RESTART", ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        body.put("error", "Unprocessable Entity");
        body.put("code", "ONBOARDING.SESSION.EXPIRED");
        body.put("message", "Onboarding session is no longer active. Please restart the onboarding flow.");
        body.put("nextAction", "RESTART");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }
}
