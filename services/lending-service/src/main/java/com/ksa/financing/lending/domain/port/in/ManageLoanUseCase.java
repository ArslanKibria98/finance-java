package com.ksa.financing.lending.domain.port.in;

import com.ksa.financing.lending.domain.model.LoanAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.model.ShariaStructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageLoanUseCase {

    LoanAggregate createLoanFromApplication(CreateLoanCommand command);

    LoanAggregate disburseLoan(DisburseLoanCommand command);

    LoanAggregate getLoan(UUID tenantId, UUID loanId);

    LoanAggregate getLoanByNumber(UUID tenantId, String loanNumber);

    List<LoanAggregate> listLoansByCustomer(UUID tenantId, UUID customerId);

    // ==================== COMMANDS ====================

    record CreateLoanCommand(
            UUID tenantId,
            LoanApplicationId applicationId,
            UUID customerId,
            UUID productId,
            String productCode,
            ShariaStructure shariaStructure,
            BigDecimal principalAmount,
            BigDecimal profitAmount,
            BigDecimal profitRate,
            int tenureMonths,
            BigDecimal installmentAmount
    ) {}

    record DisburseLoanCommand(
            UUID tenantId,
            UUID loanId,
            LocalDate disbursementDate,
            LocalDate firstDueDate,
            LocalDate maturityDate
    ) {}
}
