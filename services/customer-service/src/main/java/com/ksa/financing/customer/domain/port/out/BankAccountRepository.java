package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository {
    BankAccount save(BankAccount bankAccount);
    Optional<BankAccount> findById(UUID id);
    List<BankAccount> findByCustomerId(UUID tenantId, UUID customerId);
    Optional<BankAccount> findByCustomerIdAndIban(UUID customerId, String iban);
}
