package com.ksa.financing.risk.infrastructure.enrichment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub VPN/proxy detector — returns false by default.
 * Replace with real VPN detection provider (IPQualityScore, ip-api pro, etc.) in production.
 */
@Component
@Slf4j
public class StubVpnProxyDetector {

    public boolean isVpn(String ipAddress) {
        log.debug("Stub VPN detector: checking IP={}", ipAddress);
        return false;
    }

    public boolean isProxy(String ipAddress) {
        log.debug("Stub proxy detector: checking IP={}", ipAddress);
        return false;
    }
}
