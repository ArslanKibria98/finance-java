package com.ksa.financing.fraud.domain.model.device;

public record DeviceInfo(
    String deviceId,
    DeviceType deviceType,
    DeviceOS deviceOs,
    String osVersion,
    String deviceFingerprint,
    DeviceIntegrityStatus integrityStatus
) {}
