package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferByMobileUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateTransferByMobileService implements InitiateTransferByMobileUseCase {

    private final RecipientLookupPort recipientLookupPort;
    private final WalletRepository walletRepository;
    private final InitiateTransferUseCase initiateTransferUseCase;

    @Override
    public WalletTransfer initiateByMobile(InitiateByMobileCommand command) {
        if (command.senderKeycloakUserId() == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Sender identity not present in JWT");
        }
        if (command.receiverMobile() == null || command.receiverMobile().isBlank()) {
            throw new BusinessException(
                    "WALLET.TRANSFER.RECEIVER_MOBILE_REQUIRED",
                    "receiverMobile is required");
        }

        // Resolve sender (from JWT sub) → customerId → wallet
        var senderInfo = recipientLookupPort.lookupByKeycloakUserId(command.senderKeycloakUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.INVALID_CREDENTIALS,
                        "Sender user not found in identity service"));

        if (senderInfo.customerId() == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "Sender has no customer profile");
        }

        Wallet senderWallet = walletRepository.findByCustomerId(senderInfo.tenantId(), senderInfo.customerId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.Wallet.NOT_FOUND,
                        "Sender has no wallet"));

        // Resolve receiver (by mobile) → customerId → wallet
        var receiverInfo = recipientLookupPort.lookupByMobile(command.receiverMobile().trim())
                .orElseThrow(() -> new BusinessException(
                        "WALLET.TRANSFER.RECEIVER_NOT_FOUND",
                        "No user found for receiver mobile"));

        if (receiverInfo.customerId() == null) {
            throw new BusinessException(
                    "WALLET.TRANSFER.RECEIVER_NOT_FOUND",
                    "Receiver has no customer profile");
        }

        Wallet receiverWallet = walletRepository.findByCustomerId(receiverInfo.tenantId(), receiverInfo.customerId())
                .orElseThrow(() -> new BusinessException(
                        "WALLET.TRANSFER.RECEIVER_NO_WALLET",
                        "Receiver has no wallet"));

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new BusinessException(
                    "WALLET.TRANSFER.SELF_NOT_ALLOWED",
                    "Cannot send to your own wallet");
        }

        UUID tenantId = senderInfo.tenantId();
        UUID initiatorUserId = senderInfo.customerId();

        log.info("Mobile-based transfer resolved: senderWallet={} receiverWallet={} amount={}",
                senderWallet.getId(), receiverWallet.getId(), command.amount());

        return initiateTransferUseCase.initiate(new InitiateTransferUseCase.InitiateTransferCommand(
                tenantId,
                senderWallet.getId(),
                receiverWallet.getId(),
                null,
                command.amount(),
                command.currency(),
                command.purposeNote(),
                "P2P_MOBILE",
                command.idempotencyKey(),
                initiatorUserId,
                command.initiatorIp(),
                command.initiatorDeviceId()));
    }
}
