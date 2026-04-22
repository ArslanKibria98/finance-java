package com.ksa.financing.lending.adapter.temporal.activity;

import com.ksa.financing.lending.application.usecase.BankAccountLookupService;
import com.ksa.financing.lending.domain.model.*;
import com.ksa.financing.lending.domain.port.in.ManageLoanUseCase;
import com.ksa.financing.lending.domain.port.out.EventPublisher;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import com.ksa.financing.lending.infrastructure.client.FraudEventNotifier;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Temporal activity implementation for lending-service internal operations.
 * Each method is one transactional operation on the loan application aggregate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationActivityImpl implements LoanApplicationActivity {

    private final LoanApplicationRepository applicationRepository;
    private final ManageLoanUseCase loanUseCase;
    private final EventPublisher eventPublisher;
    private final BankAccountLookupService bankAccountLookupService;
    private final FraudEventNotifier fraudEventNotifier;

    // ══════════ APPLICATION LIFECYCLE ══════════

    @Override
    @Transactional
    public CreateApplicationResult createDraftApplication(CreateDraftInput input) {
        log.info("Activity: Creating draft application for customer: {}", input.customerId());

        var tenantId = UUID.fromString(input.tenantId());
        var applicationNumber = applicationRepository.generateApplicationNumber(tenantId);

        var aggregate = LoanApplicationAggregate.create(
                tenantId,
                applicationNumber,
                UUID.fromString(input.customerId()),
                input.nationalId(),
                input.monthlyIncome(),
                input.totalExpenses(),
                input.existingLiabilities(),
                input.adultDependents(),
                input.childDependents(),
                input.foodGroceries(),
                input.utilities(),
                input.healthcare(),
                input.communication(),
                input.housingRent(),
                input.clothingEssentials(),
                input.education(),
                input.transportation(),
                UUID.fromString(input.createdBy())
        );

        if (input.workflowId() != null) {
            aggregate.assignWorkflow(input.workflowId());
        }

        aggregate = applicationRepository.save(aggregate);
        publishEvents(aggregate);

        log.info("Draft application created: {}", applicationNumber);
        return new CreateApplicationResult(
                aggregate.getId().getValue().toString(),
                aggregate.getApplicationNumber()
        );
    }

    @Override
    @Transactional
    public void saveBasicInfo(SaveBasicInfoInput input) {
        log.info("Activity: Saving basic info for application: {}", input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());

        aggregate.submitBasicInfo(
                safeUuid(input.productId()),
                input.productCode(),
                input.productName(),
                input.shariaStructure() != null ? ShariaStructure.valueOf(input.shariaStructure()) : null,
                input.requestedAmount(),
                input.requestedTenureMonths(),
                input.purposeOfFinance(),
                null, // purposeOfFinanceOther
                input.profitRate(),
                null, // apr
                safeUuid(input.partnerId()),
                safeUuid(input.leadId()),
                UUID.fromString(input.updatedBy())
        );

        applicationRepository.save(aggregate);
        publishEvents(aggregate);
    }

    @Override
    @Transactional
    public void saveBankAccount(SaveBankAccountInput input) {
        log.info("Activity: Saving bank account for application: {}", input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());

        aggregate.saveBankAccount(
                input.bankCode(),
                input.bankName(),
                input.iban(),
                input.accountHolder(),
                input.verified(),
                UUID.fromString(input.updatedBy())
        );

        applicationRepository.save(aggregate);

        // Sync bank account to customer-service so it shows in bank-accounts list API
        String customerId = aggregate.getCustomerId() != null ? aggregate.getCustomerId().toString() : null;
        if (customerId != null) {
            bankAccountLookupService.syncBankAccountToCustomerService(
                    customerId, input.bankName(), input.bankCode(),
                    input.iban(), input.accountHolder(), null);
        }
    }

    @Override
    @Transactional
    public void recordSimahConsent(RecordConsentInput input) {
        log.info("Activity: Recording SIMAH consent for application: {}", input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());

        aggregate.recordSimahConsent(
                input.simahConsent(),
                UUID.fromString(input.updatedBy())
        );

        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void saveEligibilityResult(SaveEligibilityInput input) {
        log.info("Activity: Saving eligibility result for application: {}", input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());

        aggregate.saveEligibilityResult(
                input.eligible(),
                input.creditScore(),
                input.simahReferenceId(),
                input.verifiedSalary(),
                input.dbrBefore(),
                input.dbrAfter(),
                input.maxEligibleAmount(),
                UUID.fromString(input.updatedBy())
        );

        applicationRepository.save(aggregate);
        publishEvents(aggregate);
    }

    @Override
    @Transactional
    public void saveOffer(SaveOfferInput input) {
        log.info("Activity: Saving offer for application: {}", input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());

        aggregate.presentOffer(
                input.maxAmount(),
                input.monthlyInstallment(),
                input.totalProfit(),
                input.totalPayable(),
                input.processingFee(),
                input.adminFee(),
                UUID.fromString(input.updatedBy())
        );

        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void lockAcceptedOffer(LockOfferInput input) {
        log.info("Activity: Locking accepted offer for application: {}", input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());

        aggregate.acceptOffer(
                input.selectedAmount(),
                input.monthlyInstallment(),
                input.totalPayable(),
                input.totalProfit(),
                UUID.fromString(input.updatedBy())
        );

        applicationRepository.save(aggregate);
        publishEvents(aggregate);
    }

    @Override
    @Transactional
    public void updateStatus(UpdateStatusInput input) {
        log.info("Activity: Updating application {} status to {}", input.applicationId(), input.targetStatus());

        var aggregate = findApplication(input.tenantId(), input.applicationId());
        var updatedBy = UUID.fromString(input.updatedBy());

        switch (input.targetStatus()) {
            case "BASIC_INFO_SUBMITTED" -> {} // Handled by saveBasicInfo
            case "BANK_ACCOUNT_PENDING" -> aggregate.moveToBankAccountPending(updatedBy);
            case "BANK_ACCOUNT_VERIFIED" -> {} // Handled by saveBankAccount
            case "SIMAH_CONSENT_GIVEN" -> {} // Handled by recordSimahConsent
            case "ELIGIBILITY_CHECKING" -> aggregate.moveToEligibilityChecking(updatedBy);
            case "ELIGIBILITY_PASSED" -> {} // Handled by saveEligibilityResult
            case "OFFER_PRESENTED" -> {} // Handled by saveOffer
            case "OFFER_ACCEPTED" -> {} // Handled by lockAcceptedOffer
            case "CONTRACT_PENDING" -> aggregate.moveToContractPending(
                    LocalDateTime.now().plusHours(24), updatedBy);
            case "CONTRACT_SIGNING" -> aggregate.recordContractConsent(false, false, false, updatedBy);
            case "OTP_VERIFICATION" -> aggregate.moveToOtpVerification(updatedBy);
            case "IVR_VERIFICATION" -> aggregate.recordOtpVerified(updatedBy);
            case "CONTRACT_SIGNED" -> aggregate.recordIvrVerified(updatedBy);
            case "LOAN_CREATING" -> aggregate.moveToLoanCreating(updatedBy);
            case "DISBURSING" -> aggregate.moveToDisbursing(updatedBy);
            case "APPROVED" -> aggregate.approve(updatedBy);
            case "REJECTED" -> aggregate.reject("Workflow rejection", updatedBy);
            case "CANCELLED" -> aggregate.cancel(updatedBy);
            case "EXPIRED" -> aggregate.expire();
            default -> throw new IllegalArgumentException("Unknown target status: " + input.targetStatus());
        }

        applicationRepository.save(aggregate);
        publishEvents(aggregate);
    }

    @Override
    @Transactional
    public void setDisbursementDelay(SetDisbursementDelayInput input) {
        log.info("Activity: Setting disbursement delay {}h for application {}",
                input.disbursementDurationHours(), input.applicationId());

        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.setDisbursementDurationHours(Math.max(0, input.disbursementDurationHours()));

        if (input.scheduledAt() != null && !input.scheduledAt().isBlank()) {
            try {
                aggregate.setDisbursementScheduledAt(LocalDateTime.parse(input.scheduledAt()));
            } catch (Exception ex) {
                log.warn("Invalid scheduledAt format '{}', ignoring", input.scheduledAt());
            }
        }

        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void cancelApplication(CancelInput input) {
        log.info("Activity: Cancelling application: {} — reason: {}", input.applicationId(), input.reason());

        var tenantId = UUID.fromString(input.tenantId());
        var appId = LoanApplicationId.of(UUID.fromString(input.applicationId()));

        applicationRepository.findById(tenantId, appId).ifPresent(aggregate -> {
            try {
                var cancelledBy = UUID.fromString(input.cancelledBy());
                aggregate.cancel(cancelledBy);
                applicationRepository.save(aggregate);
            } catch (Exception e) {
                log.warn("Could not cancel application {} (may already be terminal): {}",
                        input.applicationId(), e.getMessage());
            }
        });
    }

    // ══════════ LOAN CREATION ══════════

    @Override
    @Transactional
    public LoanCreationResult createLoan(LoanCreationInput input) {
        log.info("Activity: Creating loan from application: {}", input.applicationId());

        var command = new ManageLoanUseCase.CreateLoanCommand(
                UUID.fromString(input.tenantId()),
                LoanApplicationId.of(UUID.fromString(input.applicationId())),
                UUID.fromString(input.customerId()),
                safeUuid(input.productId()),
                input.productCode(),
                ShariaStructure.valueOf(input.shariaStructure()),
                input.principalAmount(),
                input.profitAmount(),
                input.profitRate(),
                input.tenureMonths(),
                input.installmentAmount()
        );

        var loan = loanUseCase.createLoanFromApplication(command);

        // Fire non-blocking fraud event for loan creation
        try {
            fraudEventNotifier.notifyLoanApplicationCreated(
                    input.tenantId(), input.customerId(), null,
                    input.applicationId(), input.productCode(),
                    input.principalAmount(), null, null);
        } catch (Exception e) {
            log.warn("Fraud event notification failed (non-blocking): {}", e.getMessage());
        }

        return new LoanCreationResult(
                loan.getId().getValue().toString(),
                loan.getLoanNumber()
        );
    }

    @Override
    @Transactional
    public void generateAmortizationSchedule(AmortizationInput input) {
        log.info("Activity: Generating amortization schedule for loan: {}", input.loanId());
        // Schedule generation happens as part of loan creation via domain-core-sdk
        log.info("Amortization schedule generated during loan creation");
    }

    @Override
    @Transactional
    public void markLoanDisbursed(MarkDisbursedInput input) {
        log.info("Activity: Marking loan {} as disbursed", input.loanId());
        var tenantId = UUID.fromString(input.tenantId());
        var loanId = UUID.fromString(input.loanId());
        var command = new com.ksa.financing.lending.domain.port.in.ManageLoanUseCase.DisburseLoanCommand(
                tenantId, loanId,
                java.time.LocalDate.now(),
                java.time.LocalDate.now().plusMonths(1),
                null
        );
        loanUseCase.disburseLoan(command);
        log.info("Loan {} marked as disbursed", input.loanId());
    }

    // ══════════ THIRD-PARTY RESULT PERSISTENCE ══════════

    @Override
    @Transactional
    public void saveSafeWatchResult(SaveSafeWatchInput input) {
        log.info("Activity: Saving SafeWatch result for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordSafeWatchResult(input.sessionId(), input.status(), UUID.fromString(input.updatedBy()));
        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void saveMasdarResult(SaveMasdarInput input) {
        log.info("Activity: Saving Masdar result for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordMasdarResult(
                input.employerName(), input.employmentSector(), input.employmentStatus(),
                input.basicSalary(), input.totalSalary(), input.employmentStartDate(),
                UUID.fromString(input.updatedBy()));
        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void saveAmlDeclaration(SaveAmlDeclarationInput input) {
        log.info("Activity: Saving AML declaration for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordAmlDeclaration(UUID.fromString(input.updatedBy()));
        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void saveNabaNotification(SaveNabaInput input) {
        log.info("Activity: Saving NABA notification for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordNabaNotificationSent(UUID.fromString(input.updatedBy()));
        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void savePaymentGuardResult(SavePaymentGuardInput input) {
        log.info("Activity: Saving PaymentGuard result for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordPaymentGuardResult(input.sessionId(), input.status(), UUID.fromString(input.updatedBy()));
        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void saveOtpAttempt(SaveOtpAttemptInput input) {
        log.info("Activity: Recording OTP attempt for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordOtpAttempt();
        applicationRepository.save(aggregate);
    }

    @Override
    @Transactional
    public void saveIvrAttempt(SaveIvrAttemptInput input) {
        log.info("Activity: Recording IVR attempt for application: {}", input.applicationId());
        var aggregate = findApplication(input.tenantId(), input.applicationId());
        aggregate.recordIvrAttempt();
        applicationRepository.save(aggregate);
    }

    // ══════════ HELPERS ══════════

    private LoanApplicationAggregate findApplication(String tenantId, String applicationId) {
        var tid = UUID.fromString(tenantId);
        var appId = LoanApplicationId.of(UUID.fromString(applicationId));
        return applicationRepository.findById(tid, appId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
    }

    private void publishEvents(LoanApplicationAggregate aggregate) {
        if (!aggregate.getUncommittedEvents().isEmpty()) {
            eventPublisher.publishAll(aggregate.getUncommittedEvents());
            aggregate.markEventsAsCommitted();
        }
    }

    /**
     * Safely parses a string as UUID, returning null if the string is null, blank, or not a valid UUID.
     * This handles cases where product/partner IDs may be codes (e.g., "MURABAHA-001") rather than UUIDs.
     */
    private UUID safeUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse '{}' as UUID, treating as null", value);
            return null;
        }
    }
}
