package com.ksa.financing.collections.domain.model;

public record SimahReportingConfig(
        boolean enabled,
        int reportDpd,
        int defaultStatusDpd
) {

    public SimahReportingConfig {
        if (enabled) {
            if (reportDpd < 1)
                throw new IllegalArgumentException("reportDpd must be >= 1");
            if (defaultStatusDpd < reportDpd)
                throw new IllegalArgumentException("defaultStatusDpd must be >= reportDpd");
        }
    }

    public boolean shouldReport(int dpd) {
        return enabled && dpd >= reportDpd;
    }

    public boolean shouldMarkDefault(int dpd) {
        return enabled && dpd >= defaultStatusDpd;
    }

    public static SimahReportingConfig defaults() {
        return new SimahReportingConfig(true, 60, 90);
    }
}
