package com.ksa.financing.infra.audit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ksa.audit.api")
public class ApiAuditProperties {

    /** Master switch. */
    private boolean enabled = true;

    /** Capture inbound request/response bodies. */
    private boolean captureRequestBody = true;
    private boolean captureResponseBody = true;

    /** Capture outbound (third-party / inter-service) bodies. */
    private boolean captureOutboundBody = true;

    /** Cap body length (bytes). Bodies above this are truncated with marker. */
    private int maxBodySizeBytes = 10_240;

    /** Cap header value length to avoid huge tokens being stored. */
    private int maxHeaderValueLength = 512;

    /** Paths excluded from audit (Ant-style). Health, swagger, prometheus, etc. */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            "/favicon.ico"
    ));

    /** Field names whose values are masked (case-insensitive substring match). */
    private List<String> maskFields = new ArrayList<>(List.of(
            "password",
            "secret",
            "token",
            "authorization",
            "otp",
            "pin",
            "cvv",
            "cardnumber",
            "card_number",
            "nationalid",
            "national_id",
            "nid",
            "iqama",
            "iban",
            "accountnumber",
            "account_number"
    ));

    /** Headers whose values are masked entirely. */
    private List<String> maskHeaders = new ArrayList<>(List.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "proxy-authorization"
    ));

    /** Map outbound host substring -> friendly third-party name (for dashboard grouping). */
    private List<ThirdPartyMapping> thirdPartyMappings = new ArrayList<>(List.of(
            new ThirdPartyMapping("nafath", "nafath"),
            new ThirdPartyMapping("yakeen", "yakeen"),
            new ThirdPartyMapping("simah", "simah"),
            new ThirdPartyMapping("fineract", "fineract"),
            new ThirdPartyMapping("keycloak", "keycloak"),
            new ThirdPartyMapping("sadad", "sadad")
    ));

    @Getter
    @Setter
    public static class ThirdPartyMapping {
        private String hostContains;
        private String name;

        public ThirdPartyMapping() {}

        public ThirdPartyMapping(String hostContains, String name) {
            this.hostContains = hostContains;
            this.name = name;
        }
    }
}
