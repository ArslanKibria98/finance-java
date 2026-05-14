package com.ksa.financing.wallet.infrastructure.fineract;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Feature flag controlling whether wallet-service triggers Fineract sync flows.
 * Replaces the old {@code FineractWalletConfig} after Fineract calls were moved
 * to the ledger-service proxy.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ksa.wallet.fineract")
public class WalletFineractFeatureFlag {

    private boolean enabled = true;

    /**
     * 2-digit SA IBAN bank code used when deriving deterministic IBANs from
     * Fineract savings account ids. Overridable via env var FINERACT_IBAN_BANK_CODE.
     */
    private String ibanBankCode = "80";
}
