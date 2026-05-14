package com.ksa.financing.ledger;

import com.ksa.financing.ledger.application.config.LedgerAdminAdjustmentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KSA Islamic Financing Platform — Ledger Service
 *
 * Acts as the bridge between domain logic and Apache Fineract.
 * Provides double-entry accounting, journal entry posting,
 * idempotent Fineract sync, and daily reconciliation.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(LedgerAdminAdjustmentProperties.class)
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
