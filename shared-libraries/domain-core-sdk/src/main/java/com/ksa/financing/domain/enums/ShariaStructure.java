package com.ksa.financing.domain.enums;

/**
 * Sharia-compliant financing structures.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public enum ShariaStructure {
    /**
     * Commodity Monetization (3-party sale).
     * Customer buys commodity on credit, platform sells it to market for cash.
     */
    TAWARRUQ("Commodity Murabaha"),

    /**
     * Cost-plus profit financing.
     * Platform buys asset, sells to customer at disclosed markup.
     */
    MURABAHA("Cost-Plus Sale"),

    /**
     * Lease-to-own financing.
     * Platform owns asset during lease, transfers ownership at end.
     */
    IJARA("Leasing"),

    /**
     * Profit-loss sharing partnership.
     * Both parties contribute capital and share profits proportionally.
     */
    MUDARABA("Trust Partnership"),

    /**
     * Equity partnership with diminishing ownership.
     * Customer gradually buys out platform's share.
     */
    DIMINISHING_MUSHARAKA("Diminishing Partnership");

    private final String description;

    ShariaStructure(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
