package com.ksa.financing.risk.domain.model.session;

import com.ksa.financing.risk.domain.model.location.LocationData;

public record SessionPattern(
    int loginCountLast24Hours,
    boolean unusualTimeFlag,
    LocationData lastKnownLocation
) {}
