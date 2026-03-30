package com.ksa.financing.customer.adapter.rest.controller;

import com.ksa.financing.customer.application.dto.AddBankAccountRequest;
import com.ksa.financing.customer.application.dto.BankAccountResponse;
import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal endpoint for service-to-service bank account operations.
 * No JWT required — used by lending-service Temporal activities.
 */
@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
@Slf4j
public class InternalBankAccountController {

    private final ManageBankAccountsUseCase manageBankAccountsUseCase;
    private final GetCustomerUseCase getCustomerUseCase;

    @GetMapping("/exists/{nationalId}")
    public ResponseEntity<java.util.Map<String, Boolean>> existsByNationalId(
            @PathVariable String nationalId) {
        try {
            getCustomerUseCase.getByNationalId(nationalId);
            return ResponseEntity.ok(java.util.Map.of("exists", true));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("exists", false));
        }
    }

    @PostMapping("/{customerId}/bank-accounts")
    public ResponseEntity<BankAccountResponse> addBankAccount(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddBankAccountRequest request) {

        log.info("Internal: Adding bank account for customer: {}", customerId);

        // Look up customer to get their tenant_id
        Customer customer = getCustomerUseCase.getById(customerId);
        UUID tenantId = customer.getTenantId();

        ManageBankAccountsUseCase.AddBankAccountCommand command = new ManageBankAccountsUseCase.AddBankAccountCommand(
                request.bankName(),
                request.bankCode(),
                request.iban(),
                request.accountHolderName(),
                request.accountType(),
                request.isPrimary() != null ? request.isPrimary() : true,
                request.isSalaryAccount() != null ? request.isSalaryAccount() : false
        );

        BankAccount bankAccount = manageBankAccountsUseCase.addBankAccount(tenantId, customerId, command);

        var response = new BankAccountResponse(
                bankAccount.getId(),
                bankAccount.getBankName(),
                bankAccount.getBankCode(),
                bankAccount.getIban(),
                bankAccount.getAccountHolderName(),
                bankAccount.getAccountType(),
                bankAccount.isPrimary(),
                bankAccount.isSalaryAccount(),
                bankAccount.getStatus() != null ? bankAccount.getStatus().name() : "PENDING_VERIFICATION",
                null,
                bankAccount.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
