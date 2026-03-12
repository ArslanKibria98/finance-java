package com.ksa.financing.fraud.domain.model.location;

import java.math.BigDecimal;

public record GeoDistance(
    BigDecimal distanceKm,
    BigDecimal fromLatitude,
    BigDecimal fromLongitude,
    BigDecimal toLatitude,
    BigDecimal toLongitude,
    long withinMinutes
) {}
