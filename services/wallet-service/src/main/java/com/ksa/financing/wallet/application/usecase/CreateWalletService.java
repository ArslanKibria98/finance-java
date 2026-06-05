package com.ksa.financing.wallet.application.usecase;

// TODO: Move FineractSyncWorkflow interface to domain/port or application layer to fix adapter dependency (hexagonal violation)
import com.ksa.financing.wallet.adapter.temporal.workflow.FineractSyncWorkflow;
import com.ksa.financing.wallet.application.support.AccountNumberGenerator;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.ManageWalletLimitBoundsUseCase;
import com.ksa.financing.wallet.domain.port.out.EventPublisherPort;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.infrastructure.fineract.WalletFineractFeatureFlag;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class CreateWalletService implements CreateWalletUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateWalletService.class);

    private final WalletRepository walletRepository;
    private final EventPublisherPort eventPublisher;
    private final WorkflowClient workflowClient;
    private final WalletFineractFeatureFlag fineractConfig;
    private final RecipientLookupPort recipientLookupPort;
    private final ManageWalletLimitBoundsUseCase limitBoundsUseCase;
    private final String scotiaFiNumber;
    private final String scotiaTransit;

    public CreateWalletService(WalletRepository walletRepository,
                               EventPublisherPort eventPublisher,
                               WorkflowClient workflowClient,
                               WalletFineractFeatureFlag fineractConfig,
                               RecipientLookupPort recipientLookupPort,
                               ManageWalletLimitBoundsUseCase limitBoundsUseCase,
                               @Value("${ksa.wallet.scotia.fi-number:002}") String scotiaFiNumber,
                               @Value("${ksa.wallet.scotia.transit:80150}") String scotiaTransit) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
        this.workflowClient = workflowClient;
        this.fineractConfig = fineractConfig;
        this.recipientLookupPort = recipientLookupPort;
        this.limitBoundsUseCase = limitBoundsUseCase;
        this.scotiaFiNumber = scotiaFiNumber;
        this.scotiaTransit = scotiaTransit;
    }

    @Override
    @Transactional
    public Wallet create(CreateWalletCommand command) {
        if (walletRepository.existsByCustomerId(command.tenantId(), command.customerId())) {
            return walletRepository.findByCustomerId(command.tenantId(), command.customerId())
                .orElseThrow();
        }

        Wallet wallet = new Wallet();
        wallet.setTenantId(command.tenantId());
        wallet.setCustomerId(command.customerId());
        wallet.setWalletNumber("WLT" + System.currentTimeMillis() % 10000000000L);
        // Virtual Canadian-format account number used as the RTP debtor/creditor reference.
        wallet.setAccountNumber(AccountNumberGenerator.generate(
                scotiaFiNumber, scotiaTransit, walletRepository.nextAccountNumberSequence()));
        wallet.setCurrency(command.currency() != null ? command.currency() : "SAR");
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setReservedBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setDailyTopUpLimit(new BigDecimal("50000"));
        wallet.setMonthlyTopUpLimit(new BigDecimal("200000"));
        wallet.setSingleTopUpLimit(new BigDecimal("10000"));
        wallet.setTodayTopUpAmount(BigDecimal.ZERO);
        wallet.setMonthTopUpAmount(BigDecimal.ZERO);
        // Transaction (spend) limits — seed from the tenant's admin-configured defaults.
        WalletLimitBounds bounds = limitBoundsUseCase.getBounds(command.tenantId());
        wallet.setSingleTransactionLimit(bounds.getDefaultSingleLimit());
        wallet.setDailyTransactionLimit(bounds.getDefaultDailyLimit());
        wallet.setWeeklyTransactionLimit(bounds.getDefaultWeeklyLimit());
        wallet.setMonthlyTransactionLimit(bounds.getDefaultMonthlyLimit());
        wallet.setYearlyTransactionLimit(bounds.getDefaultYearlyLimit());
        // IBAN: prefer caller-supplied. Otherwise, when Fineract sync is enabled we leave
        // it null at creation and the FineractSync activity derives a deterministic IBAN
        // from the Fineract savings account id (see FineractSyncActivityImpl.linkWalletToFineract).
        // For mock/disabled mode we fall back to a random IBAN so the field stays populated.
        wallet.setIban(command.iban() != null
                ? command.iban()
                : (fineractConfig.isEnabled() ? null : generateIban()));
        applyDisplayName(wallet, command);
        wallet.setAutoDebitEnabled(true);

        Wallet saved = walletRepository.save(wallet);

        // Start Fineract sync workflow AFTER transaction commits successfully
        if (fineractConfig.isEnabled()) {
            scheduleAfterCommit(saved);
        }

        try {
            eventPublisher.publishWalletCreated(saved);
        } catch (Exception e) {
            log.warn("Failed to publish wallet-created event for walletId={} — continuing: {}",
                    saved.getId(), e.getMessage());
        }
        return saved;
    }

    private void scheduleAfterCommit(Wallet wallet) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                startFineractSyncWorkflow(wallet);
            }
        });
    }

    private void startFineractSyncWorkflow(Wallet wallet) {
        try {
            var input = new FineractSyncWorkflow.FineractSyncInput(
                    wallet.getCustomerId().toString(),
                    wallet.getWalletNumber(),
                    wallet.getCurrency()
            );

            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId("fineract-sync-" + wallet.getWalletNumber())
                    .setTaskQueue(TaskQueue.WALLET_QUEUE)
                    .setWorkflowExecutionTimeout(Duration.ofMinutes(10))
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                    .build();

            FineractSyncWorkflow workflow = workflowClient.newWorkflowStub(FineractSyncWorkflow.class, options);
            WorkflowClient.start(workflow::syncWalletToFineract, input);

            log.info("Fineract sync workflow started for wallet={}", wallet.getWalletNumber());
        } catch (Exception e) {
            log.warn("Failed to start Fineract sync workflow for wallet={}: {}. Manual retry needed.",
                    wallet.getWalletNumber(), e.getMessage());
        }
    }

    /**
     * Resolves the customer display name (merged at the NAFATH layer as
     * "<englishFirstName> <englishThirdName>") and stores all three derived
     * fields on the wallet. Strict resolution order — NO random fallback:
     *   1. {@code command.displayName()} (forwarded via customer-created event)
     *   2. Identity-service lookup (legacy producers that omit fullName)
     *   3. {@code null} — the wallet records no display name; read paths keep
     *      it null rather than inventing one.
     */
    private void applyDisplayName(Wallet wallet, CreateWalletCommand command) {
        String displayName = resolveDisplayName(command);
        if (displayName == null || displayName.isBlank()) {
            wallet.setMaskedName(null);
            wallet.setEnglishFirstName(null);
            wallet.setEnglishThirdName(null);
            return;
        }
        String trimmed = displayName.trim();
        int sep = indexOfWhitespace(trimmed);
        String first = sep < 0 ? trimmed : trimmed.substring(0, sep);
        String third = sep < 0 ? null : trimmed.substring(sep + 1).trim();
        if (third != null && third.isEmpty()) third = null;
        wallet.setEnglishFirstName(first);
        wallet.setEnglishThirdName(third);
        wallet.setMaskedName(third == null ? first : first + " " + third);
    }

    private String resolveDisplayName(CreateWalletCommand command) {
        String fromEvent = command.displayName();
        if (fromEvent != null && !fromEvent.isBlank()) {
            return fromEvent.trim();
        }
        java.util.UUID customerId = command.customerId();
        if (customerId == null) return null;
        try {
            return recipientLookupPort.lookupByCustomerId(customerId)
                    .map(info -> {
                        if (info.name() != null && !info.name().isBlank()) return info.name().trim();
                        String first = info.firstName() != null ? info.firstName().trim() : "";
                        String last = info.lastName() != null ? info.lastName().trim() : "";
                        String joined = (first + " " + last).trim();
                        return joined.isEmpty() ? null : joined;
                    })
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("Identity lookup failed during wallet creation for customerId={}: {}",
                    customerId, ex.getMessage());
            return null;
        }
    }

    private int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private String generateIban() {
        var rng = new java.util.Random();
        var sb = new StringBuilder("SA");
        sb.append(String.format("%02d", rng.nextInt(100)));
        sb.append("80"); // bank code
        for (int i = 0; i < 18; i++) sb.append(rng.nextInt(10));
        return sb.toString();
    }
}
