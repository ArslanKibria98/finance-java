package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.GetRecentRecipientsUseCase;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import com.ksa.financing.wallet.domain.port.out.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetRecentRecipientsService implements GetRecentRecipientsUseCase {

    private final WalletRepository walletRepository;
    private final WalletTransferRepository transferRepository;
    private final RecipientLookupPort recipientLookupPort;

    @Override
    @Transactional(readOnly = true)
    public List<RecentRecipient> getRecentRecipients(UUID tenantId, UUID customerId) {
        log.info("Fetching recent recipients for customerId={} tenantId={}", customerId, tenantId);

        Wallet sourceWallet = walletRepository.findByCustomerId(tenantId, customerId)
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", customerId.toString()));

        List<UUID> recentWalletIds = transferRepository.findRecentRecipientWalletIds(sourceWallet.getId(), 5);
        List<RecentRecipient> results = new ArrayList<>();

        for (UUID walletId : recentWalletIds) {
            walletRepository.findById(walletId).ifPresent(wallet -> {
                var lookup = recipientLookupPort.lookupByCustomerId(wallet.getCustomerId());

                String maskedMobile = null;
                String userStatus = null;
                boolean userEnabled = true;

                if (lookup.isPresent()) {
                    var info = lookup.get();
                    maskedMobile = info.maskedMobile();
                    userStatus = info.status();
                    userEnabled = info.enabled();
                }

                // Single source of truth: wallet.maskedName (frozen at wallet-creation
                // time from the NAFATH pool name). No transfer-row fallback, no live
                // identity-derived name, no random — strict.
                String maskedName = wallet.getMaskedName();

                boolean canReceive = wallet.getStatus() == WalletStatus.ACTIVE && userEnabled;
                String reason = canReceive ? null
                        : (wallet.getStatus() != WalletStatus.ACTIVE
                                ? "WALLET_NOT_ACTIVE_" + wallet.getStatus().name()
                                : "USER_DISABLED");

                results.add(new RecentRecipient(
                        true,
                        wallet.getId(),
                        wallet.getWalletNumber(),
                        wallet.getIban(),
                        maskedName,
                        maskedMobile,
                        wallet.getCurrency(),
                        wallet.getStatus() != null ? wallet.getStatus().name() : null,
                        userStatus,
                        canReceive,
                        reason
                ));
            });
        }

        return results;
    }
}
