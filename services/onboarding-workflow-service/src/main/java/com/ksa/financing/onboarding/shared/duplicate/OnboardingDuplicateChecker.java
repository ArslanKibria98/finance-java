package com.ksa.financing.onboarding.shared.duplicate;

import com.ksa.financing.infra.exception.ConflictException;
import com.ksa.financing.infra.exception.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Upfront duplicate detection used by every onboarding {@code /initiate} use case.
 *
 * <p>Hits {@code GET /internal/profiles/exists?email=&mobile=} on global-profile-service.
 * If either is already registered the helper throws a {@link ConflictException} with the
 * narrowest matching {@code ONBOARDING.*.ALREADY_REGISTERED} error code so the client
 * gets HTTP 409 with a stable machine-readable code.</p>
 *
 * <p>If global-profile-service is unreachable the check is skipped (fail-open) so a
 * transient downstream outage does not block onboarding — duplicates would still be
 * caught later by the unique constraints at customer creation.</p>
 */
@Component
public class OnboardingDuplicateChecker {

    private static final Logger log = LoggerFactory.getLogger(OnboardingDuplicateChecker.class);

    private final RestTemplate restTemplate;
    private final String globalProfileUrl;

    public OnboardingDuplicateChecker(RestTemplate restTemplate,
                                      @Value("${app.services.global-profile-url:http://localhost:8085}") String globalProfileUrl) {
        this.restTemplate = restTemplate;
        this.globalProfileUrl = globalProfileUrl == null ? null : globalProfileUrl.replaceAll("/+$", "");
    }

    /**
     * Throws {@link ConflictException} if the given email and/or mobile is already
     * registered in global-profile-service.
     */
    @SuppressWarnings("unchecked")
    public void assertNotAlreadyRegistered(String email, String mobile) {
        if ((email == null || email.isBlank()) && (mobile == null || mobile.isBlank())) {
            return;
        }
        // Manual URL encoding because the mobile number's '+' must NOT be sent as a space.
        StringBuilder url = new StringBuilder(globalProfileUrl + "/internal/profiles/exists");
        char sep = '?';
        if (emptyToNull(email) != null) {
            url.append(sep).append("email=").append(URLEncoder.encode(email, StandardCharsets.UTF_8));
            sep = '&';
        }
        if (emptyToNull(mobile) != null) {
            url.append(sep).append("mobile=").append(URLEncoder.encode(mobile, StandardCharsets.UTF_8));
        }

        boolean emailExists;
        boolean mobileExists;
        try {
            // Wrap in URI.create so RestTemplate does NOT re-encode our already-encoded params
            // (which would turn %40 into %2540 and break the lookup).
            URI uri = URI.create(url.toString());
            log.info("Onboarding duplicate check GET {}", uri);
            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            Map<String, Object> body = response.getBody();
            log.info("Onboarding duplicate check raw body: {}", body);
            if (body == null) {
                log.warn("global-profile-service /exists returned empty body — skipping duplicate check");
                return;
            }
            Map<String, Object> data = unwrap(body);
            emailExists = Boolean.TRUE.equals(data.get("emailExists"));
            mobileExists = Boolean.TRUE.equals(data.get("mobileExists"));
            log.info("Onboarding duplicate check: emailExists={} mobileExists={}", emailExists, mobileExists);
        } catch (Exception e) {
            log.warn("global-profile-service /exists lookup failed — skipping duplicate check: {}", e.getMessage());
            return;
        }

        if (emailExists && mobileExists) {
            throw new ConflictException(ErrorCodes.Onboarding.ACCOUNT_ALREADY_REGISTERED,
                    "An account with this email and mobile already exists");
        }
        if (emailExists) {
            throw new ConflictException(ErrorCodes.Onboarding.EMAIL_ALREADY_REGISTERED,
                    "An account with this email already exists");
        }
        if (mobileExists) {
            throw new ConflictException(ErrorCodes.Onboarding.MOBILE_ALREADY_REGISTERED,
                    "An account with this mobile number already exists");
        }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrap(Map<String, Object> envelope) {
        Object dataNode = envelope.get("data");
        if (dataNode instanceof Map<?, ?> data) {
            return (Map<String, Object>) data;
        }
        return envelope;
    }
}
