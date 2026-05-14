package com.ksa.financing.infra.audit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds {@link ApiAuditRestTemplateInterceptor} to every {@link RestTemplate}
 * bean discovered in the application context. Avoids forcing each service
 * to wire the interceptor manually.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiAuditRestTemplateRegistrar {

    private final List<RestTemplate> restTemplates;
    private final ApiAuditRestTemplateInterceptor interceptor;

    @PostConstruct
    public void register() {
        if (restTemplates == null || restTemplates.isEmpty()) {
            log.debug("No RestTemplate beans found; ApiAuditRestTemplateInterceptor not attached.");
            return;
        }
        for (RestTemplate rt : restTemplates) {
            if (rt.getInterceptors().stream().noneMatch(i -> i instanceof ApiAuditRestTemplateInterceptor)) {
                List<org.springframework.http.client.ClientHttpRequestInterceptor> merged =
                        new ArrayList<>(rt.getInterceptors());
                merged.add(interceptor);
                rt.setInterceptors(merged);
            }
        }
        log.info("Attached ApiAuditRestTemplateInterceptor to {} RestTemplate bean(s).", restTemplates.size());
    }
}
