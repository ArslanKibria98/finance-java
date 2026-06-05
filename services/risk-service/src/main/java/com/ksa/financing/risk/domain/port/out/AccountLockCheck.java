package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;

public interface AccountLockCheck {

    AccountLockResult check(AccountLockInput input);

    record AccountLockInput(String nidHash) {}

    record AccountLockResult(CheckDecision decision, boolean hasActiveLock, String lockType,
                             String lockReason, String failureReason) {}
}
