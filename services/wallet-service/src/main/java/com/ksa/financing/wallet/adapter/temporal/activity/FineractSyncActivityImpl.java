package com.ksa.financing.wallet.adapter.temporal.activity;

import com.ksa.financing.wallet.domain.iban.IbanGenerator;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.infrastructure.fineract.WalletFineractFeatureFlag;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class FineractSyncActivityImpl implements FineractSyncActivity {

    private final FineractSavingsPort fineractPort;
    private final WalletRepository walletRepository;
    private final WalletFineractFeatureFlag fineractConfig;

    @Override
    public Long lookupFineractClient(String customerId) {
        log.info("[FineractSync] Looking up Fineract client for customerId={}", customerId);
        Long clientId = fineractPort.lookupClientByExternalId(customerId);
        if (clientId != null) {
            log.info("[FineractSync] Found existing Fineract clientId={} for customerId={}", clientId, customerId);
            return clientId;
        }
        // Client not found — create in Fineract with customerId as externalId
        log.info("[FineractSync] Client not found, creating Fineract client for customerId={}", customerId);
        Long newClientId = fineractPort.createClient(customerId, null);
        log.info("[FineractSync] Created Fineract clientId={} for customerId={}", newClientId, customerId);
        return newClientId;
    }

    @Override
    public Long createSavingsAccount(Long fineractClientId, String walletNumber) {
        log.info("[FineractSync] Creating savings account: clientId={} wallet={}", fineractClientId, walletNumber);
        Long savingsId = fineractPort.createSavings(fineractClientId, walletNumber);
        log.info("[FineractSync] Savings account created: savingsId={}", savingsId);
        return savingsId;
    }

    @Override
    public void approveSavingsAccount(Long savingsId) {
        log.info("[FineractSync] Approving savings account: savingsId={}", savingsId);
        fineractPort.approveSavings(savingsId);
        log.info("[FineractSync] Savings account approved: savingsId={}", savingsId);
    }

    @Override
    public void activateSavingsAccount(Long savingsId) {
        log.info("[FineractSync] Activating savings account: savingsId={}", savingsId);
        fineractPort.activateSavings(savingsId);
        log.info("[FineractSync] Savings account activated: savingsId={}", savingsId);
    }

    @Override
    public void linkWalletToFineract(String walletNumber, Long savingsId) {
        log.info("[FineractSync] Linking wallet={} to savingsId={}", walletNumber, savingsId);
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> Activity.wrap(new IllegalStateException(
                        "Wallet not found: " + walletNumber)));

        String derivedIban = IbanGenerator.fromFineractSavingsId(savingsId, fineractConfig.getIbanBankCode());

        wallet.setFineractSavingsAccountId(savingsId);
        wallet.setIban(derivedIban);
        wallet.setLedgerSynced(true);
        wallet.setLastLedgerSyncAt(Instant.now());
        walletRepository.save(wallet);
        log.info("[FineractSync] Wallet {} linked to Fineract savings {} (iban={})",
                walletNumber, savingsId, derivedIban);
    }

    @Override
    public void deleteSavingsAccount(Long savingsId) {
        log.info("[FineractSync] Compensation: deleting savings account savingsId={}", savingsId);
        fineractPort.deleteSavings(savingsId);
    }
}
