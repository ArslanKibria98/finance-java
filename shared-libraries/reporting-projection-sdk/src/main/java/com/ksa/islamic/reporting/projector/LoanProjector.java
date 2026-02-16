package com.ksa.islamic.reporting.projector;

import com.ksa.islamic.reporting.event.LoanEvents;
import com.ksa.islamic.reporting.readmodel.LoanSummaryReadModel;
import com.ksa.islamic.reporting.repository.LoanReadRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projector for loan events into LoanSummaryReadModel.
 * Handles all loan-related events and updates the denormalized read model.
 */
@Slf4j
@Component
public class LoanProjector extends EventProjector<BaseProjectableEvent> {

    private final LoanReadRepository loanReadRepository;

    public LoanProjector(
            ProjectionCheckpointRepository checkpointRepository,
            MeterRegistry meterRegistry,
            Tracer tracer,
            LoanReadRepository loanReadRepository) {
        super(checkpointRepository, meterRegistry, tracer);
        this.loanReadRepository = loanReadRepository;
    }

    @Override
    protected void projectIncremental(BaseProjectableEvent event) {
        log.debug("Projecting event incrementally: {}", event.getEventType());

        if (event instanceof LoanEvents.LoanApproved) {
            handleLoanApproved((LoanEvents.LoanApproved) event);
        } else if (event instanceof LoanEvents.LoanDisbursed) {
            handleLoanDisbursed((LoanEvents.LoanDisbursed) event);
        } else if (event instanceof LoanEvents.PaymentReceived) {
            handlePaymentReceived((LoanEvents.PaymentReceived) event);
        } else if (event instanceof LoanEvents.LoanOverdue) {
            handleLoanOverdue((LoanEvents.LoanOverdue) event);
        } else if (event instanceof LoanEvents.LoanClosed) {
            handleLoanClosed((LoanEvents.LoanClosed) event);
        } else if (event instanceof LoanEvents.LoanRestructured) {
            handleLoanRestructured((LoanEvents.LoanRestructured) event);
        }
    }

    @Override
    protected void projectRebuild(BaseProjectableEvent event) {
        log.debug("Rebuilding projection for event: {}", event.getEventType());
        // For rebuild, we would typically query all events for an aggregate
        // and rebuild the entire read model from scratch
        // This is useful when the read model structure changes
        projectIncremental(event); // For now, just do incremental
    }

    @Override
    protected ProjectionStrategy determineStrategy(BaseProjectableEvent event) {
        // For loan events, we typically use incremental updates
        // Rebuild is only needed for major structural changes
        return ProjectionStrategy.INCREMENTAL;
    }

    @Override
    public String[] getTopics() {
        return new String[]{
                "domain.loan.approved",
                "domain.loan.disbursed",
                "domain.loan.payment",
                "domain.loan.overdue",
                "domain.loan.closed",
                "domain.loan.restructured"
        };
    }

    @Override
    public String getGroupId() {
        return "loan-projector-group";
    }

    @Transactional
    protected void handleLoanApproved(LoanEvents.LoanApproved event) {
        log.info("Handling LoanApproved event for loan: {}", event.getLoanAccountNumber());

        LoanSummaryReadModel loan = LoanSummaryReadModel.builder()
                .loanId(event.getAggregateId())
                .tenantId(event.getTenantId())
                .loanAccountNumber(event.getLoanAccountNumber())
                .customerId(event.getCustomerId())
                .customerName(event.getCustomerName())
                .productType(event.getProductType())
                .status("APPROVED")
                .principalAmount(event.getPrincipalAmount())
                .outstandingPrincipal(event.getPrincipalAmount())
                .interestRate(event.getInterestRate())
                .approvalDate(event.getApprovalDate())
                .numberOfInstallments(event.getTermMonths())
                .installmentsPaid(0)
                .daysOverdue(0)
                .overdueAmount(BigDecimal.ZERO)
                .overdueInstallments(0)
                .riskCategory("LOW")
                .paymentPerformanceScore(100)
                .earlyPayments(0)
                .latePayments(0)
                .build();

        loanReadRepository.save(loan);
    }

    @Transactional
    protected void handleLoanDisbursed(LoanEvents.LoanDisbursed event) {
        log.info("Handling LoanDisbursed event for loan: {}", event.getLoanAccountNumber());

        loanReadRepository.findById(event.getAggregateId()).ifPresent(loan -> {
            loan.setStatus("DISBURSED");
            loan.setDisbursementDate(event.getDisbursementDate());
            loan.setOutstandingPrincipal(event.getDisbursementAmount());

            // Calculate maturity date based on term
            if (loan.getNumberOfInstallments() != null) {
                loan.setMaturityDate(event.getDisbursementDate().plusMonths(loan.getNumberOfInstallments()));
            }

            // Set first payment date (typically one month after disbursement)
            loan.setNextPaymentDate(event.getDisbursementDate().plusMonths(1));

            loanReadRepository.save(loan);
        });
    }

    @Transactional
    protected void handlePaymentReceived(LoanEvents.PaymentReceived event) {
        log.info("Handling PaymentReceived event for loan: {}", event.getLoanAccountNumber());

        loanReadRepository.findById(event.getAggregateId()).ifPresent(loan -> {
            // Update outstanding amounts
            loan.setOutstandingPrincipal(event.getOutstandingAfter());

            // Update paid amounts
            BigDecimal currentPaidPrincipal = loan.getPaidPrincipal() != null ?
                loan.getPaidPrincipal() : BigDecimal.ZERO;
            loan.setPaidPrincipal(currentPaidPrincipal.add(event.getPrincipalPortion()));

            BigDecimal currentPaidInterest = loan.getPaidInterest() != null ?
                loan.getPaidInterest() : BigDecimal.ZERO;
            loan.setPaidInterest(currentPaidInterest.add(event.getInterestPortion()));

            // Update late fees if any
            if (event.getLateFee() != null && event.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentLateFees = loan.getLateFees() != null ?
                    loan.getLateFees() : BigDecimal.ZERO;
                loan.setLateFees(currentLateFees.add(event.getLateFee()));
                loan.setLatePayments(loan.getLatePayments() + 1);
            } else {
                loan.setEarlyPayments(loan.getEarlyPayments() + 1);
            }

            // Update dates
            loan.setLastPaymentDate(event.getPaymentDate());
            loan.setNextPaymentDate(event.getPaymentDate().plusMonths(1));

            // Update installments
            loan.setInstallmentsPaid(loan.getInstallmentsPaid() + 1);

            // Reset overdue if caught up
            if (event.getOutstandingAfter().compareTo(BigDecimal.ZERO) == 0 ||
                loan.getDaysOverdue() <= 0) {
                loan.setDaysOverdue(0);
                loan.setOverdueAmount(BigDecimal.ZERO);
                loan.setOverdueInstallments(0);
            }

            // Update status if fully paid
            if (event.getOutstandingAfter().compareTo(BigDecimal.ZERO) == 0) {
                loan.setStatus("CLOSED");
            }

            loanReadRepository.save(loan);
        });
    }

    @Transactional
    protected void handleLoanOverdue(LoanEvents.LoanOverdue event) {
        log.info("Handling LoanOverdue event for loan: {}", event.getLoanAccountNumber());

        loanReadRepository.findById(event.getAggregateId()).ifPresent(loan -> {
            loan.setDaysOverdue(event.getDaysOverdue());
            loan.setOverdueAmount(event.getOverdueAmount());
            loan.setOverdueInstallments(event.getOverdueInstallments());

            // Update risk category based on days overdue
            if (event.getDaysOverdue() > 90) {
                loan.setRiskCategory("CRITICAL");
                loan.setStatus("WRITTEN_OFF");
            } else if (event.getDaysOverdue() > 60) {
                loan.setRiskCategory("HIGH");
            } else if (event.getDaysOverdue() > 30) {
                loan.setRiskCategory("MEDIUM");
            }

            // Update collection status
            if (event.getDaysOverdue() > 60) {
                loan.setCollectionStatus("LEGAL");
            } else if (event.getDaysOverdue() > 30) {
                loan.setCollectionStatus("IN_COLLECTION");
            } else if (event.getDaysOverdue() > 0) {
                loan.setCollectionStatus("REMINDER_SENT");
            }

            // Adjust payment performance score
            int performanceScore = Math.max(0, 100 - (event.getDaysOverdue() * 2));
            loan.setPaymentPerformanceScore(performanceScore);

            loanReadRepository.save(loan);
        });
    }

    @Transactional
    protected void handleLoanClosed(LoanEvents.LoanClosed event) {
        log.info("Handling LoanClosed event for loan: {}", event.getLoanAccountNumber());

        loanReadRepository.findById(event.getAggregateId()).ifPresent(loan -> {
            loan.setStatus("CLOSED");
            loan.setOutstandingPrincipal(BigDecimal.ZERO);
            loan.setOverdueAmount(BigDecimal.ZERO);
            loan.setDaysOverdue(0);
            loan.setOverdueInstallments(0);
            loan.setCollectionStatus("CLOSED");

            loanReadRepository.save(loan);
        });
    }

    @Transactional
    protected void handleLoanRestructured(LoanEvents.LoanRestructured event) {
        log.info("Handling LoanRestructured event for loan: {}", event.getLoanAccountNumber());

        loanReadRepository.findById(event.getAggregateId()).ifPresent(loan -> {
            loan.setPrincipalAmount(event.getNewPrincipalAmount());
            loan.setOutstandingPrincipal(event.getNewPrincipalAmount());
            loan.setInterestRate(event.getNewInterestRate());
            loan.setNumberOfInstallments(event.getNewTermMonths());
            loan.setMaturityDate(LocalDate.now().plusMonths(event.getNewTermMonths()));
            loan.setStatus("RESTRUCTURED");

            // Reset performance metrics after restructuring
            loan.setDaysOverdue(0);
            loan.setOverdueAmount(BigDecimal.ZERO);
            loan.setOverdueInstallments(0);
            loan.setCollectionStatus("CURRENT");

            loanReadRepository.save(loan);
        });
    }
}