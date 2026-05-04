package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.GetTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetTransferService implements GetTransferUseCase {

    private final WalletTransferRepository transferRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletTransfer getById(UUID tenantId, UUID transferId) {
        return transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("WalletTransfer", transferId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransfer> listByWallet(UUID walletId) {
        List<WalletTransfer> sent = transferRepository.findBySourceWallet(walletId);
        List<WalletTransfer> received = transferRepository.findByDestinationWallet(walletId);
        return java.util.stream.Stream.concat(sent.stream(), received.stream())
                .sorted((a, b) -> b.getInitiatedAt().compareTo(a.getInitiatedAt()))
                .toList();
    }
}
