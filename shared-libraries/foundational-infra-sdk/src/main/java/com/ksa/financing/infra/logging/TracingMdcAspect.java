package com.ksa.financing.infra.logging;

import com.ksa.financing.infra.security.TenantContextHolder;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TracingMdcAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object addTracingContext(ProceedingJoinPoint joinPoint) throws Throwable {
        Span currentSpan = Span.current();

        try {
            MDC.put("traceId", currentSpan.getSpanContext().getTraceId());
            MDC.put("spanId", currentSpan.getSpanContext().getSpanId());

            String tenantId = TenantContextHolder.getTenantId();
            if (tenantId != null) {
                MDC.put("tenantId", tenantId);
            }

            String userId = TenantContextHolder.getUserId();
            if (userId != null) {
                MDC.put("userId", userId);
            }

            return joinPoint.proceed();
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }
}
