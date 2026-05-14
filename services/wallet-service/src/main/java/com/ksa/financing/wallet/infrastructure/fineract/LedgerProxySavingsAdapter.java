package com.ksa.financing.wallet.infrastructure.fineract;

import com.ksa.financing.infra.security.TenantContextHolder;
import com.ksa.financing.ledger.client.savings.LedgerFineractSavingsClient;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Adapter that implements wallet's {@link FineractSavingsPort} by delegating to the
 * ledger-service Fineract proxy via {@link LedgerFineractSavingsClient}.
 * <p>
 * Wallet-service no longer talks to Fineract directly — every call goes through
 * ledger-service so audit trail, idempotency, and journal-entry posting stay centralised.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerProxySavingsAdapter implements FineractSavingsPort {

    private final LedgerFineractSavingsClient client;

    @Value("${ksa.wallet.fineract.office-id:1}")
    private Integer officeId;

    @Value("${ksa.wallet.fineract.savings-product-id:1}")
    private Integer savingsProductId;

    @Value("${ksa.wallet.fineract.default-tenant-id:00000000-0000-0000-0000-000000000001}")
    private String defaultTenantId;

    @Override
    public Long lookupClientByExternalId(String externalId) {
        return client.lookupClientByExternalId(currentTenantId(), externalId);
    }

    @Override
    public Long createClient(String externalId, String displayName) {
        return client.createClient(currentTenantId(), externalId, displayName, officeId,
                idempotencyFor("client-create", externalId));
    }

    @Override
    public Long createSavings(Long fineractClientId, String walletNumber) {
        return client.createSavings(currentTenantId(), fineractClientId, walletNumber,
                savingsProductId, idempotencyFor("savings-create", walletNumber));
    }

    @Override
    public void approveSavings(Long savingsId) {
        client.approveSavings(currentTenantId(), savingsId,
                idempotencyFor("savings-approve", String.valueOf(savingsId)));
    }

    @Override
    public void activateSavings(Long savingsId) {
        client.activateSavings(currentTenantId(), savingsId,
                idempotencyFor("savings-activate", String.valueOf(savingsId)));
    }

    @Override
    public void deleteSavings(Long savingsId) {
        client.deleteSavings(currentTenantId(), savingsId);
    }

    @Override
    public SavingsAccountInfo getAccountInfo(Long savingsId) {
        var info = client.getAccountInfo(currentTenantId(), savingsId);
        return new SavingsAccountInfo(
                info.savingsId(), info.clientId(), info.externalId(), info.status(),
                info.accountBalance(), info.availableBalance(), info.currency());
    }

    @Override
    public Long deposit(Long savingsId, BigDecimal amount, String externalReference) {
        return client.deposit(currentTenantId(), savingsId, amount, externalReference,
                externalReference != null ? externalReference : idempotencyFor("deposit", savingsId + "-" + amount));
    }

    @Override
    public Long withdraw(Long savingsId, BigDecimal amount, String externalReference) {
        return client.withdraw(currentTenantId(), savingsId, amount, externalReference,
                externalReference != null ? externalReference : idempotencyFor("withdraw", savingsId + "-" + amount));
    }

    @Override
    public Long transferBetweenSavings(Long fromClientId, Long fromSavingsId,
                                       Long toClientId, Long toSavingsId,
                                       BigDecimal amount, String description) {
        return client.transferBetweenSavings(currentTenantId(),
                officeId.longValue(), fromClientId, fromSavingsId,
                officeId.longValue(), toClientId, toSavingsId,
                amount, description,
                idempotencyFor("transfer", fromSavingsId + "-" + toSavingsId + "-" + amount));
    }

    @Override
    public List<SavingsTransaction> getTransactions(Long savingsId) {
        return client.getTransactions(currentTenantId(), savingsId).stream()
                .map(t -> new SavingsTransaction(
                        t.transactionId(), t.transactionType(), t.amount(),
                        t.runningBalance(), t.date(), t.reversed(), t.paymentDetails()))
                .toList();
    }

    private UUID currentTenantId() {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant.isBlank()) {
            tenant = defaultTenantId;
        }
        return UUID.fromString(tenant);
    }

    private String idempotencyFor(String op, String suffix) {
        return "wallet:" + op + ":" + suffix;
    }
}
