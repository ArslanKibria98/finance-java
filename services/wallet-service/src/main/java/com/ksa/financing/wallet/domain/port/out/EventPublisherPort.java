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
}
