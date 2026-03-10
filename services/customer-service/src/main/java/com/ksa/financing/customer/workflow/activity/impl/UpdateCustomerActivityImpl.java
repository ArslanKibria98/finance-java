package com.ksa.financing.customer.workflow.activity.impl;

import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.port.in.UpdateCustomerUseCase;
import com.ksa.islamic.orchestration.activity.customer.UpdateCustomerActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class UpdateCustomerActivityImpl implements UpdateCustomerActivity {

    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final ManageBankAccountsUseCase manageBankAccountsUseCase;

    @Override
    public UpdateCustomerResult updateWithAdditionalInfo(UpdateCustomerInput input) {
        log.info("Updating customer profile for customerId={}", input.customerId());
        try {
            UUID tenantId;
            try {
                tenantId = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantId = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }
            UUID customerId = UUID.fromString(input.customerId());

            updateCustomerUseCase.update(tenantId, customerId,
                    new UpdateCustomerUseCase.UpdateCustomerCommand(
                            input.email(),
                            null,
                            null,
                            null,
                            input.city(),
                            input.region(),
                            null
                    )
            );

            if (input.iban() != null && !input.iban().isBlank()) {
                try {
                    manageBankAccountsUseCase.addBankAccount(tenantId, customerId,
                            new ManageBankAccountsUseCase.AddBankAccountCommand(
                                    input.bankName(),
                                    input.bankCode(),
                                    input.iban(),
                                    input.accountHolderName(),
                                    "CURRENT",
                                    true,
                                    true
                            )
                    );
                } catch (Exception e) {
                    log.warn("Bank account creation failed for customerId={}: {}", input.customerId(), e.getMessage());
                }
            }

            return new UpdateCustomerResult(true);
        } catch (Exception e) {
            log.error("Customer update failed for customerId={}: {}", input.customerId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
