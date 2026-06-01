package com.ksa.financing.onboarding.foreign.domain.model;

import com.ksa.financing.onboarding.domain.model.DeviceInfo;

import java.io.Serializable;

public record ForeignDataConfirmedSignal(
        String surname,
        String givenName,
        String nationality,
        String dateOfBirth,
        String passportNumber,
        String issueDate,
        String expiryDate,
        String homeAddress,
        String countryOfOrigin,
        String residentialCountry,
        DeviceInfo deviceInfo
) implements Serializable {
}
