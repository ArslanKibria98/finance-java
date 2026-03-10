package com.ksa.financing.risk.domain.model.location;

import java.math.BigDecimal;

public record LocationData(
    BigDecimal latitude,
    BigDecimal longitude,
    String ipAddress,
    String ipCountry,
    String ipCity,
    String gpsCountry,
    String gpsCity,
    boolean vpnDetected,
    boolean proxyDetected
) {}
