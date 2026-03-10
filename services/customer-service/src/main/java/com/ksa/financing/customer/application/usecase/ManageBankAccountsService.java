package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.BankAccountStatus;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.port.out.BankAccountRepository;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ManageBankAccountsService implements ManageBankAccountsUseCase {

    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;

    public ManageBankAccountsService(BankAccountRepository bankAccountRepository,
                                     CustomerRepository customerRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public BankAccount addBankAccount(UUID tenantId, UUID customerId, AddBankAccountCommand command) {
        customerRepository.findById(tenantId, customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        BankAccount account = new BankAccount();
        account.setTenantId(tenantId);
        account.setCustomerId(customerId);
        account.setBankName(command.bankName());
        account.setBankCode(command.bankCode());
        account.setIban(command.iban());
        account.setAccountHolderName(command.accountHolderName());
        account.setAccountType(command.accountType());
        account.setPrimary(command.isPrimary());
        account.setSalaryAccount(command.isSalaryAccount());
        account.setStatus(BankAccountStatus.PENDING_VERIFICATION);
        java.time.Instant now = java.time.Instant.now();
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        return bankAccountRepository.save(account);
    }

    @Override
    public List<BankAccount> getBankAccounts(UUID tenantId, UUID customerId) {
        return bankAccountRepository.findByCustomerId(tenantId, customerId);
    }
}
