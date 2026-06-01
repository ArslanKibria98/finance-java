package com.ksa.financing.onboarding.canada.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record DocSelectedSignal(
        DocumentType documentType,
        DeviceInfo deviceInfo
) implements Serializable {
}
