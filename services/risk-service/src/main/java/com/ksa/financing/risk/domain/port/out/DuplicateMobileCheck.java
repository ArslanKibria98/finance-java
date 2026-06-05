package com.ksa.financing.risk.domain.port.out;

public interface DuplicateMobileCheck {

    DuplicateMobileResult check(DuplicateMobileInput input);

    record DuplicateMobileInput(String mobileHash, String mobileNumber, String sessionId) {}

    record DuplicateMobileResult(boolean duplicate, String failureReason) {}
}
