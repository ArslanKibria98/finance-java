package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.LoanAggregate;
import com.ksa.financing.lending.domain.model.LoanId;
import com.ksa.financing.lending.domain.port.in.ManageLoanUseCase;
import com.ksa.financing.lending.domain.port.out.EventPublisher;
import com.ksa.financing.lending.domain.port.out.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ManageLoanUseCaseImpl implements ManageLoanUseCase {

    private final LoanRepository loanRepository;
    private final EventPublisher eventPublisher;

    @Override
    public LoanAggregate createLoanFromApplication(CreateLoanCommand command) {
        log.info("Creating loan from application: {} for tenant: {}",
                command.applicationId(), command.tenantId());

        var loanNumber = loanRepository.generateLoanNumber(command.tenantId());

        var loan = LoanAggregate.create(
                command.tenantId(),
                loanNumber,
                command.applicationId(),
                command.customerId(),
                command.productId(),
                command.productCode(),
                command.shariaStructure(),
                command.principalAmount(),
                command.profitAmount(),
                command.feeAmount(),
                command.profitRate(),
                command.tenureMonths(),
                command.installmentAmount()
        );

        var events = new java.util.ArrayList<>(loan.getUncommittedEvents());
        loan = loanRepository.save(loan);

        eventPublisher.publishAll(events);
        loan.markEventsAsCommitted();

        log.info("Created loan: {} from application: {} ({} events published)",
                loan.getLoanNumber(), command.applicationId(), events.size());
        return loan;
    }

    @Override
    public LoanAggregate disburseLoan(DisburseLoanCommand command) {
        log.info("Disbursing loan: {}", command.loanId());

        var loan = findLoan(command.tenantId(), command.loanId());

        loan.disburse(command.disbursementDate(), command.firstDueDate(), command.maturityDate());

        var events = new java.util.ArrayList<>(loan.getUncommittedEvents());
        loan = loanRepository.save(loan);

        eventPublisher.publishAll(events);
        loan.markEventsAsCommitted();

        log.info("Disbursed loan: {} ({} events published)", loan.getLoanNumber(), events.size());
        return loan;
    }

    @Override
    @Transactional(readOnly = true)
    public LoanAggregate getLoan(UUID tenantId, UUID loanId) {
        log.debug("Getting loan: {}", loanId);
        return findLoan(tenantId, loanId);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanAggregate getLoanByNumber(UUID tenantId, String loanNumber) {
        log.debug("Getting loan by number: {}", loanNumber);
        return loanRepository.findByLoanNumber(tenantId, loanNumber)
                .orElseThrow(() -> NotFoundException.forEntity("Loan", loanNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanAggregate> listLoansByCustomer(UUID tenantId, UUID customerId, PageQuery query) {
        log.debug("Listing loans for customer: {}", customerId);
        return loanRepository.findByCustomer(tenantId, customerId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoanAggregate> listAllLoans(UUID tenantId, PageQuery query) {
        log.debug("Listing all loans for tenant: {}", tenantId);
        return loanRepository.findAllByTenant(tenantId, query);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanAggregate getLoanByApplicationId(UUID tenantId, UUID applicationId) {
        log.debug("Getting loan by applicationId: {}", applicationId);
        return loanRepository.findByApplicationId(tenantId, applicationId)
                .orElseThrow(() -> NotFoundException.forEntity("Loan for application", applicationId.toString()));
    }

    private LoanAggregate findLoan(UUID tenantId, UUID loanId) {
        return loanRepository.findById(tenantId, LoanId.of(loanId))
                .orElseThrow(() -> NotFoundException.forEntity("Loan", loanId.toString()));
    }
}
