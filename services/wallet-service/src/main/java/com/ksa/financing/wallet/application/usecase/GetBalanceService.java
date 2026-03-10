package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetBalanceService implements GetBalanceUseCase {

    private final WalletRepository walletRepository;

    public GetBalanceService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Wallet getByWalletId(UUID walletId) {
        return walletRepository.findById(walletId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }

    @Override
    public Wallet getByCustomerId(UUID tenantId, UUID customerId) {
        return walletRepository.findByCustomerId(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found for customer: " + customerId));
    }
}
