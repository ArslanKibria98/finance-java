package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 2: CIF/NID Lookup.
 *
 * Queries internal customer records by NID hash to determine if the customer
 * is NEW, EXISTING (route to login/reactivation), or BLOCKED (silent hard block).
 */
@ActivityInterface
public interface CifLookupActivity {

    @ActivityMethod(name = "CifLookup")
    CifLookupResult lookup(CifLookupInput input);

    record CifLookupInput(
        String nidHash
    ) {}

    record CifLookupResult(
        CifStatus status,
        String customerId,
        String failureReason
    ) {}
}
