package com.ksa.financing.wallet.domain.port.out;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;

public interface EventPublisherPort {
    void publishWalletCreated(Wallet wallet);
    void publishWalletTopUp(Wallet wallet, java.math.BigDecimal amount);
    void publishTransferInitiated(WalletTransfer transfer);
    void publishTransferCompleted(WalletTransfer transfer);
    void publishTransferFailed(WalletTransfer transfer);
    void publishTransferReversed(WalletTransfer transfer);
    void publishWithdrawalInitiated(WalletWithdrawal withdrawal);
    void publishWithdrawalCompleted(WalletWithdrawal withdrawal);
    void publishWithdrawalFailed(WalletWithdrawal withdrawal);
    void publishWithdrawalCompensated(WalletWithdrawal withdrawal);

    /**
     * Receiver-side notification for an external/Scotia settlement that credited one of our wallets.
     * The internal wallet-to-wallet flow emits TRANSFER_RECEIVED via {@link #publishTransferCompleted};
     * external settlements have no {@link WalletTransfer}, so this carries the same FUNDS_RECEIVED payload.
     */
    void publishExternalSettlementReceived(java.util.UUID tenantId, java.util.UUID customerId,
                                           java.util.UUID walletId, java.util.UUID transferId,
                                           String transferNumber, java.math.BigDecimal amount,
                                           String currency, String senderMaskedName,
                                           String recipientMaskedName, String purposeNote);

    /**
     * Sender-side notification for an external/Scotia settlement that debited one of our wallets.
     * Carries the FUNDS_SENT payload (topic {@code financing.wallet.transfer.completed}); customerId = the debited (sending) customer.
     */
    void publishExternalSettlementSent(java.util.UUID tenantId, java.util.UUID customerId,
                                       java.util.UUID walletId, java.util.UUID transferId,
                                       String transferNumber, java.math.BigDecimal amount,
                                       String currency, String senderMaskedName,
                                       String recipientMaskedName, String purposeNote);
}
