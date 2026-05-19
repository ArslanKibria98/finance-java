package com.ksa.financing.wallet.domain.port.in;

import com.ksa.financing.infra.pagination.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface GetTransactionHistoryUseCase {

    PageResponse<TransactionItem> getHistory(UUID walletId, int page, int size);

    record TransactionItem(
            String id,                  // Fineract txId or transferId
            String type,                // TRANSFER_OUT / TRANSFER_IN / TOP_UP / WITHDRAWAL / FEE / OTHER
            String direction,           // CREDIT / DEBIT
            BigDecimal amount,
            BigDecimal balanceAfter,
            String status,              // COMPLETED / PROCESSING / FAILED / REVERSED
            Counterparty counterparty,
            String purposeNote,
            String transferNumber,
            String fineractTransactionId,
            Instant timestamp
    ) {}

    record Counterparty(
            UUID walletId,
            String walletNumber,
            UUID customerId,
            String maskedName,
            String maskedMobile
    ) {}
}
