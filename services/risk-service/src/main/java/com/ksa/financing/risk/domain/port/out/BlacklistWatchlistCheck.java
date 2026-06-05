package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;

public interface BlacklistWatchlistCheck {

    BlacklistResult check(BlacklistInput input);

    record BlacklistInput(String nationalId, String mobileNumber, String nidHash, String mobileHash) {}

    record BlacklistResult(CheckDecision decision, boolean blacklisted, boolean watchlisted,
                           String matchType, String failureReason) {}
}
