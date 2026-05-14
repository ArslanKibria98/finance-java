package com.ksa.financing.ledger.client.savings;

import lombok.Getter;

/**
 * Thrown when a call to the ledger-service Fineract proxy fails or returns a non-2xx status.
 */
@Getter
public class LedgerProxyException extends RuntimeException {

    private final int status;

    public LedgerProxyException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
