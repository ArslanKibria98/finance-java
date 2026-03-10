package com.ksa.financing.risk.domain.model.device;

public record DeviceInfo(
    String deviceId,
    DeviceType deviceType,
    DeviceOS deviceOs,
    String osVersion,
    String deviceFingerprint,
    DeviceIntegrityStatus integrityStatus
) {}
