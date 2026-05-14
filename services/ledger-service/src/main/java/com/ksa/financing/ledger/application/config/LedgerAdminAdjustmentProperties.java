package com.ksa.financing.ledger.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GL account (COA code) used as the opposite leg for admin-initiated single-account movements.
 */
@ConfigurationProperties(prefix = "ledger.admin-adjustment")
public record LedgerAdminAdjustmentProperties(
        String offsetAccountCode
) {}
