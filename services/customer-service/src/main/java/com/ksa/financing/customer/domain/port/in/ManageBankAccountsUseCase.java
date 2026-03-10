package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.BankAccount;
import java.util.List;
import java.util.UUID;

public interface ManageBankAccountsUseCase {
    BankAccount addBankAccount(UUID tenantId, UUID customerId, AddBankAccountCommand command);
    List<BankAccount> getBankAccounts(UUID tenantId, UUID customerId);

    record AddBankAccountCommand(
        String bankName,
        String bankCode,
        String iban,
        String accountHolderName,
        String accountType,
        boolean isPrimary,
        boolean isSalaryAccount
    ) {}
}
