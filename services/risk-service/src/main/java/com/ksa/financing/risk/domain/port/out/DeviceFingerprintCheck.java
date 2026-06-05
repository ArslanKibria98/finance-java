package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;

public interface DeviceFingerprintCheck {

    DeviceFingerprintResult check(DeviceFingerprintInput input);

    record DeviceFingerprintInput(String deviceId, String deviceFingerprint, String nid, String nidHash, String mobileNumber, String mobileHash) {}

    record DeviceFingerprintResult(CheckDecision decision, int nidAssociationCount, boolean deviceBlocked,
                                   boolean velocityExceeded, int attemptCount, String failureReason) {}
}
