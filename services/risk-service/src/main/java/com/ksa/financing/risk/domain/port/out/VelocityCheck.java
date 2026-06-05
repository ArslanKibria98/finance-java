package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;

public interface VelocityCheck {

    VelocityResult check(VelocityInput input);

    record VelocityInput(String ipAddress, String deviceId, String nidHash, String mobileHash) {}

    record VelocityResult(CheckDecision decision, boolean exceeded, String exceededRule,
                          int cooldownSeconds, String failureReason) {}
}
