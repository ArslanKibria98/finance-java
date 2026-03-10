package com.ksa.financing.wallet.adapter.temporal.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow that syncs a wallet to Fineract core banking.
 *
 * Steps: Lookup Client -> Create Savings -> Approve -> Activate -> Link Wallet
 * Compensation: If approve/activate fails, delete the created savings account.
 */
@WorkflowInterface
public interface FineractSyncWorkflow {

    @WorkflowMethod
    void syncWalletToFineract(FineractSyncInput input);

    @QueryMethod
    String getStatus();

    record FineractSyncInput(
            String customerId,
            String walletNumber,
            String currency
    ) {}
}
