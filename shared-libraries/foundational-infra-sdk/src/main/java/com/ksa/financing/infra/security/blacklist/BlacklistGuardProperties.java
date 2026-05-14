package com.ksa.financing.infra.security.blacklist;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "ksa.blacklist")
public class BlacklistGuardProperties {

    /** Enables the blacklist middleware filter globally. */
    private boolean enabled = true;

    /** Redis key prefix for blacklist entries. Pattern: {prefix}:{type}:{valueHash} */
    private String cacheKeyPrefix = "blacklist";

    /**
     * Behavior when Redis is unavailable.
     * false (default) → fail-closed: block request (secure)
     * true            → fail-open:   allow request (availability over security)
     */
    private boolean failOpen = false;

    /**
     * Token-only mode (default true). Every blacklist signal MUST come from the JWT.
     * Headers and remote-IP fallbacks are ignored. The identity-service is responsible
     * for embedding mobile / national_id / device_id claims into the JWT at login time.
     */
    private boolean tokenOnly = true;

    /** Types to evaluate. Defaults to all types except IP. */
    private Set<BlacklistType> checkTypes = EnumSet.of(
            BlacklistType.USER, BlacklistType.MOBILE, BlacklistType.NID, BlacklistType.DEVICE);

    /** Ant-style paths skipped by the filter. */
    private List<String> skipPatterns = new ArrayList<>(List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    ));

    /** JWT claim names probed for the user's mobile / phone number. */
    private List<String> mobileClaims = new ArrayList<>(List.of(
            "mobile_number", "phone_number", "phoneNumber", "mobile"
    ));

    /** JWT claim names probed for the user's national ID. */
    private List<String> nidClaims = new ArrayList<>(List.of(
            "national_id", "nationalId", "nid"
    ));

    /** JWT claim names probed for the device id (added by identity-service at login). */
    private List<String> deviceClaims = new ArrayList<>(List.of(
            "device_id", "deviceId", "device"
    ));

    /**
     * Header fallbacks for device id. Only used when {@link #tokenOnly} is false.
     * Kept for legacy callers (mobile SDK that sends X-Device-Id in addition to login).
     */
    private List<String> deviceIdHeaders = new ArrayList<>(List.of(
            "X-Device-Id", "X-Device-ID", "Device-Id"
    ));

    /** Topic name used to publish audit events of blocked requests (optional). */
    private String auditTopic = "financing.security.blocked_request";
}
