package com.ksa.financing.wallet.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled crons (IBFT reconciliation). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
