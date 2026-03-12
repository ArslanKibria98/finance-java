package com.ksa.financing.fraud.domain.model.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionEvent(
    TransactionType transactionType,
    BigDecimal amount,
    String currency,
    LocalDateTime timestamp,
    String customerId,
    String loanId,
    String ibanFrom,
    String ibanTo,
    PaymentSource paymentSource
) {}
