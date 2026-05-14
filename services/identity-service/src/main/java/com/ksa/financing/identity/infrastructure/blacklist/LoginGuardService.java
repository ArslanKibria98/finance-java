package com.ksa.financing.identity.infrastructure.blacklist;

import com.ksa.financing.infra.security.blacklist.BlacklistCacheService;
import com.ksa.financing.infra.security.blacklist.BlacklistEntry;
import com.ksa.financing.infra.security.blacklist.BlacklistType;
import com.ksa.financing.infra.security.blacklist.BlacklistViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pre-token blacklist check used by every IDS login / registration flow.
 *
 * <p>Order of evaluation: USER → NID → MOBILE → DEVICE. First hit wins.
 * Throws a {@link BlacklistViolationException} which the SDK's
 * {@code GlobalExceptionHandler} maps to a localized HTTP 403 response.</p>
 *
 * <p>Source of truth = Redis (populated by risk-service). All values are
 * SHA-256 hashed by {@link BlacklistCacheService} so plain values never hit
 * the network or logs in the lookup path.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginGuardService {

    private final BlacklistCacheService cacheService;

    public void verify(String userId, String nationalId, String mobileNumber, String deviceId) {
        check(BlacklistType.USER, userId);
        check(BlacklistType.NID, nationalId);
        check(BlacklistType.MOBILE, mobileNumber);
        check(BlacklistType.DEVICE, deviceId);
    }

    public void verifyByNid(String nationalId) {
        check(BlacklistType.NID, nationalId);
    }

    public void verifyByMobile(String mobileNumber) {
        check(BlacklistType.MOBILE, mobileNumber);
    }

    public void verifyByDevice(String deviceId) {
        check(BlacklistType.DEVICE, deviceId);
    }

    private void check(BlacklistType type, String value) {
        if (value == null || value.isBlank()) return;
        BlacklistEntry hit = cacheService.lookup(type, value);
        if (hit == null) return;

        log.warn("Login guard BLOCK: type={}, reason={}", type, hit.reason());
        throw new BlacklistViolationException(type, hit.reason() != null ? hit.reason() : "blacklisted");
    }
}
