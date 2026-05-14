package com.ksa.financing.kycadapter.infrastructure.stub;

import com.ksa.financing.kycadapter.infrastructure.middleware.MiddlewareApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tahakuk mobile-ownership verification adapter.
 *
 * Now routes through middleware-third-party (apiCode = TAHAQUQ_VERIFY_MOBILE)
 * so every call is persisted in client_request_test (or _dev / _prod
 * depending on the configured middleware client environment) for audit.
 *
 * The method signature is preserved so existing callers
 * (e.g., VerifyMobileService) need no changes — the response shape stays
 * the same: {verified, carrier, verifiedAt, nationalId, mobileNumber, ...}
 * with extra fields from the upstream Tahaquq payload passed through.
 */
@Component
public class TahakukStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(TahakukStubAdapter.class);

    private static final String API_CODE = "TAHAQUQ_VERIFY_MOBILE";

    private final MiddlewareApiClient middlewareApiClient;

    public TahakukStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public Map<String, Object> verifyMobileOwnership(String nationalId, String mobileNumber) {
        log.info("Tahaquq via middleware: verifying nationalId={}, mobile={}",
                maskId(nationalId), maskMobile(mobileNumber));

        var requestBody = Map.<String, Object>of(
                "nationalId", nationalId,
                "mobileNumber", mobileNumber
        );

        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CODE, requestBody, nationalId, mobileNumber, null);

        // Normalize to the contract the downstream VerifyMobileService expects.
        // Tahaquq mock returns "isOwner" – map it to "verified" for backwards compat.
        boolean verified = Boolean.TRUE.equals(upstream.get("isOwner"))
                || Boolean.TRUE.equals(upstream.get("verified"));

        Map<String, Object> result = new LinkedHashMap<>(upstream);
        result.put("verified", verified);
        result.putIfAbsent("nationalId", nationalId);
        result.putIfAbsent("mobileNumber", mobileNumber);
        result.putIfAbsent("verifiedAt", Instant.now().toString());
        result.putIfAbsent("carrier", "STC");

        log.info("Tahaquq result: verified={}, nationalId={}", verified, maskId(nationalId));
        return result;
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "***";
        return "***" + mobile.substring(mobile.length() - 4);
    }
}
