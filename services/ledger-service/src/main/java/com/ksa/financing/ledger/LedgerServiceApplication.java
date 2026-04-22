package com.ksa.financing.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
public class LedgerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
