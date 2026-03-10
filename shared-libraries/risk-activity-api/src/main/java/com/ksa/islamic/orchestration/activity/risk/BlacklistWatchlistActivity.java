package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 4: Internal Blacklist / Watchlist.
 *
 * Checks NID hash and mobile hash against:
 * (a) nid_blacklist / mobile_blacklist - permanently banned
 * (b) internal_watchlist - flagged for enhanced monitoring
 *
 * Blacklist match = silent hard block with compliance notification.
 * Watchlist match = proceed with ENHANCED_MONITORING flag.
 */
@ActivityInterface
public interface BlacklistWatchlistActivity {

    @ActivityMethod(name = "BlacklistWatchlistCheck")
    BlacklistResult check(BlacklistInput input);

    record BlacklistInput(
        String nidHash,
        String mobileHash
    ) {}

    record BlacklistResult(
        CheckDecision decision,
        boolean blacklisted,
        boolean watchlisted,
        String matchType,
        String failureReason
    ) {}
}
