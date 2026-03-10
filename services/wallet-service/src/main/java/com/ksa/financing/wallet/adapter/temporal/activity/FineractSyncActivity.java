package com.ksa.financing.wallet.adapter.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activities for syncing wallet savings accounts to Fineract.
 * Each method is a separate activity so Temporal can retry individually.
 */
@ActivityInterface
public interface FineractSyncActivity {

    @ActivityMethod
    Long lookupFineractClient(String customerId);

    @ActivityMethod
    Long createSavingsAccount(Long fineractClientId, String walletNumber);

    @ActivityMethod
    void approveSavingsAccount(Long savingsId);

    @ActivityMethod
    void activateSavingsAccount(Long savingsId);

    @ActivityMethod
    void linkWalletToFineract(String walletNumber, Long savingsId);

    /** Compensation: delete an unapproved/unactivated savings account */
    @ActivityMethod
    void deleteSavingsAccount(Long savingsId);
}
