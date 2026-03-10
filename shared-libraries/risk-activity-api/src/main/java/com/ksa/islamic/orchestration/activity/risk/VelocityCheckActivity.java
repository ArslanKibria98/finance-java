package com.ksa.islamic.orchestration.activity.risk;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Check 7: Velocity Checks (Device, IP, Onboarding Attempts).
 *
 * Rate limiting using Redis counters:
 * (a) Same IP: Max 10 onboarding attempts per IP per hour
 * (b) Same device: Max 5 attempts per device per 24 hours
 * (c) Same NID: Max 3 attempts per NID per 24 hours
 * (d) Same mobile: Max 3 attempts per mobile per 24 hours
 * (e) Global: Spike detection - if attempts exceed 200% of hourly baseline
 *
 * Any threshold exceeded = temporary block with cooldown.
 */
@ActivityInterface
public interface VelocityCheckActivity {

    @ActivityMethod(name = "VelocityCheck")
    VelocityResult check(VelocityInput input);

    record VelocityInput(
        String ipAddress,
        String deviceId,
        String nidHash,
        String mobileHash
    ) {}

    record VelocityResult(
        CheckDecision decision,
        boolean exceeded,
        String exceededRule,
        long cooldownSeconds,
        String failureReason
    ) {}
}
