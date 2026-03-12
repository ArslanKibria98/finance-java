package com.ksa.financing.fraud.domain.model.session;

import com.ksa.financing.fraud.domain.model.location.LocationData;

public record SessionPattern(
    int loginCountLast24Hours,
    boolean unusualTimeFlag,
    LocationData lastKnownLocation
) {}
