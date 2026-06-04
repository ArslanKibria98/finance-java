package com.ksa.financing.onboarding.shared.sullis;

import java.io.Serializable;

/**
 * Business context propagated from the onboarding workflow into a Sullis KYC call.
 *
 * <p>Forwarded to middleware-third-party as {@code X-Mobile-Number}, {@code X-National-Id},
 * {@code X-Customer-Id}, {@code X-Application-Id}, {@code X-Context-Type} and
 * {@code X-Idempotency-Key} so the api-audit log + persisted {@code client_request} row
 * carry the customer link for the Customer-360 Kibana dashboards.</p>
 *
 * <p>Any field may be {@code null} — the workflow passes whatever is known at the call
 * site (mobile is always known, customerId only after profile creation).</p>
 */
public record SullisContext(
        String mobileNumber,
        String nationalId,
        String customerId,
        String applicationId,
        String contextType,
        String idempotencyKey) implements Serializable {

    public static SullisContext empty() {
        return new SullisContext(null, null, null, null, null, null);
    }
}
