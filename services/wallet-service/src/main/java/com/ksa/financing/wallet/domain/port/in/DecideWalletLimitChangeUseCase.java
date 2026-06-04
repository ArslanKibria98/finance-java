package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.wallet.domain.model.LimitRequestStatus;
import com.ksa.financing.wallet.domain.model.WalletLimitChangeRequest;
import java.util.List;
import java.util.UUID;

/** Admin: approve / reject pending limit-change requests and list them. */
public interface DecideWalletLimitChangeUseCase {

    WalletLimitChangeRequest approve(ApproveCommand command);

    WalletLimitChangeRequest reject(RejectCommand command);

    List<WalletLimitChangeRequest> list(UUID tenantId, LimitRequestStatus status);

    record ApproveCommand(UUID tenantId, UUID requestId, UUID decisionBy, String notes) {}

    record RejectCommand(UUID tenantId, UUID requestId, UUID decisionBy, String reason, String notes) {}
}
