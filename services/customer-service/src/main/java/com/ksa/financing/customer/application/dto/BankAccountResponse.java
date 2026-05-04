package com.ksa.financing.customer.application.dto;

import java.time.Instant;
import java.util.UUID;

public record BankAccountResponse(
    UUID id,
    String bankName,
    String bankCode,
    String nameEn,
    String nameAr,
    String iban,
    String maskedIban,
    String accountHolderName,
    String accountType,
    boolean isPrimary,
    boolean isSalaryAccount,
    boolean salaryAccount,
    String status,
    Instant verifiedAt,
    Instant createdAt,
    int sortOrder
) {}
