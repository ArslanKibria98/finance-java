package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 6: Device Fingerprint (Internal DB).
 *
 * Captures device fingerprint from SDK and queries device_registry:
 * (a) Is this device associated with > 3 different NIDs? (Identity farming)
 * (b) Is this device on the blocked device list?
 * (c) How many onboarding attempts from this device in last 24 hours?
 *
 * Blocked device = hard block.
 * > 3 NIDs = hard block + identity farming flag.
 * > 5 attempts/24h = soft block with cooldown.
 */
@ActivityInterface
public interface DeviceFingerprintActivity {

    @ActivityMethod(name = "DeviceFingerprintCheck")
    DeviceFingerprintResult check(DeviceFingerprintInput input);

    record DeviceFingerprintInput(
        String deviceId,
        String deviceFingerprint
    ) {}

    record DeviceFingerprintResult(
        CheckDecision decision,
        int nidAssociationCount,
        boolean deviceBlocked,
        boolean velocityExceeded,
        int attemptCount,
        String failureReason
    ) {}
}
