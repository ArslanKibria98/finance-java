package com.ksa.financing.wallet.domain.iso;

/**
 * ISO 20022 ServiceLevel codes — execution priority / SLA.
 * <p>
 *   NURG → Non-urgent (default, T+0/T+1)
 *   URGP → Urgent payment (instant rails / SARIE same-day)
 *   SDVA → Same Day Value
 *   PRPT → Priority real-time
 */
public enum ServiceLevel {
    NURG,
    URGP,
    SDVA,
    PRPT;

    public static ServiceLevel parseOrDefault(String input) {
        if (input == null || input.isBlank()) return NURG;
        try {
            return valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NURG;
        }
    }
}
