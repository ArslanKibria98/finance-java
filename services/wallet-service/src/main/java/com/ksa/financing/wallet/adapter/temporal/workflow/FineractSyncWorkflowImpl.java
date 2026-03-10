package com.ksa.financing.wallet.adapter.temporal.workflow;

import com.ksa.financing.wallet.adapter.temporal.activity.FineractSyncActivity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Workflow implementation that syncs a wallet savings account to Fineract.
 *
 * Saga compensation: if approve or activate fails after savings creation,
 * the created savings account is deleted from Fineract.
 *
 * Each step is a separate Temporal activity with its own retry policy.
 */
public class FineractSyncWorkflowImpl implements FineractSyncWorkflow {

    private static final Logger log = Workflow.getLogger(FineractSyncWorkflowImpl.class);

    private static final RetryOptions FINERACT_RETRY = RetryOptions.newBuilder()
            .setMaximumAttempts(5)
            .setInitialInterval(Duration.ofSeconds(2))
            .setMaximumInterval(Duration.ofSeconds(30))
            .setBackoffCoefficient(2.0)
            .build();

    private static final ActivityOptions ACTIVITY_OPTIONS = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(FINERACT_RETRY)
            .build();

    private final FineractSyncActivity activities =
            Workflow.newActivityStub(FineractSyncActivity.class, ACTIVITY_OPTIONS);

    private String status = "INITIATED";

    @Override
    public void syncWalletToFineract(FineractSyncInput input) {
        log.info("Starting Fineract sync for wallet={} customer={}", input.walletNumber(), input.customerId());

        Long savingsId = null;

        try {
            // Step 1: Lookup Fineract client
            status = "LOOKING_UP_CLIENT";
            Long fineractClientId = activities.lookupFineractClient(input.customerId());

            // Step 2: Create savings account
            status = "CREATING_SAVINGS";
            savingsId = activities.createSavingsAccount(fineractClientId, input.walletNumber());

            // Step 3: Approve savings account
            status = "APPROVING_SAVINGS";
            activities.approveSavingsAccount(savingsId);

            // Step 4: Activate savings account
            status = "ACTIVATING_SAVINGS";
            activities.activateSavingsAccount(savingsId);

            // Step 5: Link wallet to Fineract savings account
            status = "LINKING_WALLET";
            activities.linkWalletToFineract(input.walletNumber(), savingsId);

            status = "COMPLETED";
            log.info("Fineract sync completed: wallet={} savingsId={}", input.walletNumber(), savingsId);

        } catch (Exception e) {
            log.error("Fineract sync failed for wallet={} at step={}: {}",
                    input.walletNumber(), status, e.getMessage());

            // Saga compensation: delete savings if it was created but approve/activate failed
            if (savingsId != null && !"LINKING_WALLET".equals(status)) {
                status = "COMPENSATING";
                try {
                    activities.deleteSavingsAccount(savingsId);
                    log.info("Compensation completed: deleted savingsId={}", savingsId);
                } catch (Exception ce) {
                    log.error("Compensation failed for savingsId={}: {}", savingsId, ce.getMessage());
                }
            }

            status = "FAILED";
            throw e;
        }
    }

    @Override
    public String getStatus() {
        return status;
    }
}
