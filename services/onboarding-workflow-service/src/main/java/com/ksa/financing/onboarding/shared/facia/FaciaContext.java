package com.ksa.financing.onboarding.shared.facia;

/**
 * Business context propagated from the onboarding workflow into a Facia call.
 *
 * <p>The middleware uses these to populate {@code business.mobile}, {@code business.customerId}
 * and {@code business.applicationId} on the audit event so the Customer-360 Kibana
 * dashboards can join a phone number to the underlying Facia document/face-match call
 * and the persisted {@code client_request_{test|dev|prod}} row.</p>
 *
 * <p>Any field may be {@code null} — the workflow passes whatever is known at the
 * point of call (mobile is always known, customerId only after profile creation).</p>
 */
public record FaciaContext(
        String mobileNumber,
        String nationalId,
        String customerId,
        String applicationId,
        String contextType,
        String idempotencyKey) {

    public static FaciaContext empty() {
        return new FaciaContext(null, null, null, null, null, null);
    }
}
