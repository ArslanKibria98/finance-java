package com.ksa.financing.ledger.client.savings;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public client interface for Fineract savings operations routed through ledger-service.
 * <p>
 * Domain services bind their {@code FineractSavingsPort} (or equivalent) to this interface.
 * Implementations in this SDK perform the HTTP call to ledger-service Fineract proxy
 * endpoints — never to Fineract directly.
 */
public interface LedgerFineractSavingsClient {

    Long lookupClientByExternalId(UUID tenantId, String externalId);

    Long createClient(UUID tenantId, String externalId, String displayName,
                      Integer officeId, String idempotencyKey);

    Long createSavings(UUID tenantId, Long fineractClientId, String walletNumber,
                       Integer savingsProductId, String idempotencyKey);

    void approveSavings(UUID tenantId, Long savingsId, String idempotencyKey);

    void activateSavings(UUID tenantId, Long savingsId, String idempotencyKey);

    void deleteSavings(UUID tenantId, Long savingsId);

    SavingsAccountInfo getAccountInfo(UUID tenantId, Long savingsId);

    Long deposit(UUID tenantId, Long savingsId, BigDecimal amount,
                 String externalReference, String idempotencyKey);

    Long withdraw(UUID tenantId, Long savingsId, BigDecimal amount,
                  String externalReference, String idempotencyKey);

    /** Place an amount hold (Fineract holdAmount). Returns the hold transaction id. */
    Long holdAmount(UUID tenantId, Long savingsId, BigDecimal amount,
                    String externalReference, String idempotencyKey);

    /** Release a previously held amount (Fineract releaseAmount) by hold transaction id. */
    void releaseHold(UUID tenantId, Long savingsId, Long holdTransactionId, String idempotencyKey);

    Long transferBetweenSavings(UUID tenantId,
                                Long fromOfficeId, Long fromClientId, Long fromSavingsId,
                                Long toOfficeId,   Long toClientId,   Long toSavingsId,
                                BigDecimal amount, String description, String idempotencyKey);

    List<SavingsTransaction> getTransactions(UUID tenantId, Long savingsId);

    record SavingsAccountInfo(
            Long savingsId,
            Long clientId,
            String externalId,
            String status,
            BigDecimal accountBalance,
            BigDecimal availableBalance,
            String currency
    ) {}

    record SavingsTransaction(
            Long transactionId,
            String transactionType,
            BigDecimal amount,
            BigDecimal runningBalance,
            String date,
            boolean reversed,
            String paymentDetails
    ) {}
}
