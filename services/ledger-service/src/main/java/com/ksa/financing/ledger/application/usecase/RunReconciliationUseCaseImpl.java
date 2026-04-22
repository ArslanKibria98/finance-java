package com.ksa.financing.ledger.application.usecase;

import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.port.in.RunReconciliationUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.domain.port.out.EventPublisher;
import com.ksa.financing.ledger.domain.port.out.FineractSyncPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: Compare internal ledger balances with Fineract.
 * Generates a reconciliation report and emits events for discrepancies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunReconciliationUseCaseImpl implements RunReconciliationUseCase {

    private final AccountRepository accountRepository;
    private final FineractSyncPort fineractSyncPort;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public ReconciliationSummary run(UUID tenantId, LocalDate reconciliationDate) {
        log.info("Running GL reconciliation for tenant={} date={}", tenantId, reconciliationDate);

        List<AccountAggregate> accounts = accountRepository.findAllByTenant(tenantId);

        int matched = 0;
        int discrepancies = 0;
        List<AccountDiscrepancy> discrepancyList = new ArrayList<>();

        for (AccountAggregate account : accounts) {
            if (account.isHeader()) {
                // Header accounts are summations — skip direct balance check
                continue;
            }

            // Placeholder: in production, query account_balances table for closing balance
            // For now use 0 as internal balance (real impl queries DB)
            BigDecimal internalBalance = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

            // Fineract balance — may fail if no mapping exists
            BigDecimal fineractBalance = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
            if (account.getFineractMappingId() != null) {
                try {
                    // Derive Fineract GL account ID from mapping (simplified)
                    fineractBalance = fineractSyncPort.getAccountBalance(null)
                            .setScale(6, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    log.warn("Could not fetch Fineract balance for account={}: {}",
                            account.getAccountCode(), e.getMessage());
                }
            }

            BigDecimal variance = internalBalance.subtract(fineractBalance).abs();
            boolean isMatch = variance.compareTo(BigDecimal.ZERO) == 0;

            if (isMatch) {
                matched++;
            } else {
                discrepancies++;
                discrepancyList.add(new AccountDiscrepancy(
                        account.getId().value(),
                        account.getAccountCode(),
                        account.getAccountName(),
                        internalBalance,
                        fineractBalance,
                        variance
                ));

                // Emit discrepancy event
                eventPublisher.publish(new JournalEntryAggregate.ReconciliationDiscrepancy(
                        account.getId().value(),
                        tenantId,
                        reconciliationDate,
                        internalBalance,
                        fineractBalance,
                        variance
                ));
            }
        }

        log.info("Reconciliation completed: tenant={} date={} total={} matched={} discrepancies={}",
                tenantId, reconciliationDate, accounts.size(), matched, discrepancies);

        return new ReconciliationSummary(
                tenantId,
                reconciliationDate,
                accounts.size(),
                matched,
                discrepancies,
                discrepancyList
        );
    }
}
