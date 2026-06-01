package com.ksa.financing.onboarding.canada.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record DocConfirmedSignal(
        String surname,
        String givenName,
        String nationality,
        String dateOfBirth,
        String documentNumber,
        String homeAddress,
        DeviceInfo deviceInfo
) implements Serializable {
}
