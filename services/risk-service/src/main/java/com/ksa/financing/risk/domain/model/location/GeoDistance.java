package com.ksa.financing.risk.domain.model.location;

import java.math.BigDecimal;

public record GeoDistance(
    BigDecimal distanceKm,
    BigDecimal fromLatitude,
    BigDecimal fromLongitude,
    BigDecimal toLatitude,
    BigDecimal toLongitude,
    long withinMinutes
) {}
