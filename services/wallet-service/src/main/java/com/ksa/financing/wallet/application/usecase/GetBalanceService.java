package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import com.ksa.financing.wallet.domain.port.out.FineractSavingsPort;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Returns wallet metadata + balance.
 * Source of truth for balance = Fineract (Option A).
 * If Fineract is unreachable, returns the cached wallet_db value with a log warning.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetBalanceService implements GetBalanceUseCase {

    private final WalletRepository walletRepository;
    private final RecipientLookupPort recipientLookupPort;
    private final FineractSavingsPort fineractPort;

    @Override
    public Wallet getByWalletId(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
        return enrichWithFineractBalance(wallet);
    }

    @Override
    public Wallet getByCustomerId(UUID tenantId, UUID customerId) {
        Wallet wallet = walletRepository.findByCustomerId(tenantId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for customer: " + customerId));
        return enrichWithFineractBalance(wallet);
    }

    @Override
    public Wallet getByMobile(UUID tenantId, String mobileNumber) {
        var lookup = recipientLookupPort.lookupByMobile(mobileNumber)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for mobile: " + mobileNumber));

        if (lookup.customerId() == null) {
            throw new IllegalArgumentException("No customer ID associated with mobile: " + mobileNumber);
        }

        Wallet wallet = walletRepository.findByCustomerId(tenantId, lookup.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for mobile: " + mobileNumber));
        return enrichWithFineractBalance(wallet);
    }

    private Wallet enrichWithFineractBalance(Wallet wallet) {
        if (wallet.getFineractSavingsAccountId() == null) {
            log.debug("Wallet {} has no Fineract savings account; returning local balance",
                    wallet.getId());
            return wallet;
        }
        try {
            FineractSavingsPort.SavingsAccountInfo info =
                    fineractPort.getAccountInfo(wallet.getFineractSavingsAccountId());
            BigDecimal available = info.availableBalance() != null ? info.availableBalance() : BigDecimal.ZERO;
            BigDecimal account = info.accountBalance() != null ? info.accountBalance() : available;
            wallet.setAvailableBalance(available);
            wallet.setReservedBalance(account.subtract(available).max(BigDecimal.ZERO));
            wallet.setTotalBalance(account);
        } catch (Exception ex) {
            log.warn("Fineract balance fetch failed for wallet={} (savingsId={}): {} — returning local cache",
                    wallet.getId(), wallet.getFineractSavingsAccountId(), ex.getMessage());
        }
        return wallet;
    }
}
