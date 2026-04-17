package com.ksa.financing.customer.workflow.activity.impl;

import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.RiskGrade;
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

            String email = (input.email() != null && !input.email().isBlank()) ? input.email() : "customer@example.com";
            updateCustomerUseCase.update(tenantId, customerId,
                    new UpdateCustomerUseCase.UpdateCustomerCommand(
                            null,
                            null,
                            null,
                            null,
                            email,
                            null,
                            null,
                            null,
                            input.city(),
                            input.region(),
                            null,
                            null  // profilePicture — not updated via workflow activity
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

    @Override
    public UpdateKycStatusResult updateKycStatus(UpdateKycStatusInput input) {
        log.info("Updating KYC status for customerId={} to {}", input.customerId(), input.kycStatus());
        try {
            UUID tenantId;
            try {
                tenantId = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantId = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }
            UUID customerId = UUID.fromString(input.customerId());
            KycStatus kycStatus = KycStatus.valueOf(input.kycStatus());

            updateCustomerUseCase.updateKycStatus(tenantId, customerId, kycStatus);
            log.info("KYC status updated to {} for customerId={}", input.kycStatus(), input.customerId());
            return new UpdateKycStatusResult(true);
        } catch (Exception e) {
            log.error("KYC status update failed for customerId={}: {}", input.customerId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }

    @Override
    public UpdateRiskGradeResult updateRiskGrade(UpdateRiskGradeInput input) {
        log.info("Updating risk grade for customerId={}, riskScore={}", input.customerId(), input.riskScore());
        try {
            UUID tenantId;
            try {
                tenantId = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantId = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }
            UUID customerId = UUID.fromString(input.customerId());

            // Aligned with AML thresholds: LOW 0-30, MEDIUM 31-41, HIGH 42+
            // Score 999 = PEP dominant override → HIGH
            RiskGrade grade;
            int score = input.riskScore();
            if (score >= 42) grade = RiskGrade.HIGH;
            else if (score >= 31) grade = RiskGrade.MEDIUM;
            else grade = RiskGrade.LOW;

            updateCustomerUseCase.updateRiskGrade(tenantId, customerId, grade);

            // If score >= 42 (HIGH), also set pepFlag for PEP dominant overrides (score=999)
            if (score >= 999) {
                updateCustomerUseCase.updatePepFlag(tenantId, customerId, true);
                log.info("PEP flag set to true for customerId={} (dominant override score={})", input.customerId(), score);
            }

            log.info("Risk grade updated to {} (score={}) for customerId={}", grade, score, input.customerId());
            return new UpdateRiskGradeResult(true, grade.name());
        } catch (Exception e) {
            log.error("Risk grade update failed for customerId={}: {}", input.customerId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
